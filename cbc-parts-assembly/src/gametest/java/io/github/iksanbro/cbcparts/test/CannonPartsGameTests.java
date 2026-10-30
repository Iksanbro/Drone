package io.github.iksanbro.cbcparts.test;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.fluids.spout.FillingBySpout;
import com.simibubi.create.content.fluids.transfer.FillingRecipe;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import com.simibubi.create.content.kinetics.press.PressingRecipe;
import com.simibubi.create.content.kinetics.saw.CuttingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedRecipe;
import io.github.iksanbro.cbcparts.CBCPartsAssembly;
import io.github.iksanbro.cbcparts.Config;
import io.github.iksanbro.cbcparts.GeneratedParts;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;
import rbasamoyai.createbigcannons.cannons.ICannonBlockEntity;
import rbasamoyai.createbigcannons.crafting.BlockRecipe;
import rbasamoyai.createbigcannons.crafting.BlockRecipesManager;

@GameTestHolder(CBCPartsAssembly.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CannonPartsGameTests {
    private static final String CBC = "createbigcannons";
    private static final String CBCAT = "cbc_at";
    private static final String NW = "cbcmodernwarfare";

    // ---------------------------------------------------------------------------------
    // Recipes

    /**
     * Runs every sequenced assembly recipe of the mod step by step, looking each step up the
     * same way the deployer, press and saw of Create do. Fails if a step is not found, if a
     * different recipe answers (ambiguous start), if the end result is wrong, or if the recipe
     * does not use exactly 3x the metal of the casting and at most one other consumed item, if
     * the same machine works twice in a row, or if the chain has other than 1-2 saws.
     */
    @GameTest(template = "empty")
    public static void everyRecipeBuildsItsPartForTripleMetal(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<String> errors = new ArrayList<>();
        int checked = 0;
        for (RecipeHolder<?> holder : level.getRecipeManager().getRecipes()) {
            if (!holder.id().getNamespace().equals(CBCPartsAssembly.MOD_ID)) continue;
            if (holder.value() instanceof DeployerApplicationRecipe) continue; // second stage, run below
            if (holder.value() instanceof CuttingRecipe) continue; // mould recipe, checked by the world test
            if (holder.id().getPath().startsWith("fixes/")) continue; // fixed copies of broken addon recipes
            if (!(holder.value() instanceof SequencedAssemblyRecipe recipe)) {
                errors.add(holder.id() + ": unexpected recipe type " + holder.value().getClass().getSimpleName());
                continue;
            }
            checked++;
            String problem = runRecipe(level, holder.id(), recipe);
            if (problem != null) errors.add(holder.id() + ": " + problem);
        }
        int expected = loadedParts().size();
        CBCPartsAssembly.LOGGER.info("[test] sequenced recipes checked: {} (expected {})", checked, expected);
        if (checked != expected) errors.add("found " + checked + " recipes, expected " + expected);
        if (!errors.isEmpty()) helper.fail(String.join("\n", errors));
        helper.succeed();
    }

    private static List<GeneratedParts.UnfinishedPart> loadedParts() {
        ModList mods = ModList.get();
        return GeneratedParts.ALL.stream().filter(p -> p.requiredMods().stream().allMatch(mods::isLoaded)).toList();
    }

    private static Optional<GeneratedParts.UnfinishedPart> partFor(ResourceLocation result) {
        String path = result.getPath().startsWith("incomplete_") ? result.getPath().substring("incomplete_".length()) : result.getPath();
        ResourceLocation finished = ResourceLocation.fromNamespaceAndPath(result.getNamespace(), path);
        return loadedParts().stream()
                .filter(p -> p.modId().equals(finished.getNamespace()) && p.targetPath().equals(finished.getPath()))
                .findFirst();
    }

    /** Metal in one item in mB: 90 for an ingot, 810 for a storage block, 0 otherwise. */
    private static int metalOf(ItemStack stack) {
        return stack.getTags().mapToInt(tag -> {
            if (!tag.location().getNamespace().equals("c")) return 0;
            String path = tag.location().getPath();
            if (path.startsWith("ingots/")) return 90;
            if (path.startsWith("storage_blocks/")) return 810;
            return 0;
        }).max().orElse(0);
    }

    private static String runRecipe(ServerLevel level, ResourceLocation id, SequencedAssemblyRecipe recipe) {
        ItemStack[] bases = recipe.getIngredient().getItems();
        if (bases.length == 0) return "base ingredient has no items";
        ItemStack item = bases[0].copy();
        ItemStack expected = recipe.getResultItem(level.registryAccess());
        Optional<GeneratedParts.UnfinishedPart> part = partFor(BuiltInRegistries.ITEM.getKey(expected.getItem()));
        if (part.isEmpty()) return "makes " + expected + ", which is not in the part list";

        int metal = metalOf(item);
        int poured = 0;
        boolean pressed = false, sawed = false;
        int otherItems = metal == 0 ? 1 : 0;
        List<SequencedRecipe<?>> sequence = recipe.getSequence();
        int total = recipe.getLoops() * sequence.size();
        Class<?> previousMachine = null;
        int saws = 0;
        for (int i = 0; i < total; i++) {
            ProcessingRecipe<?, ?> step = sequence.get(i % sequence.size()).getRecipe();
            if (step.getClass() == previousMachine) return "step " + i + " repeats " + step.getClass().getSimpleName();
            previousMachine = step.getClass();
            if (step instanceof CuttingRecipe) saws++;
            Optional<? extends RecipeHolder<? extends ProcessingRecipe<?, ?>>> found;
            if (step instanceof DeployerApplicationRecipe deploy) {
                ItemStack[] held = deploy.getRequiredHeldItem().getItems();
                if (held.length == 0) return "step " + i + ": held item ingredient is empty";
                if (!deploy.shouldKeepHeldItem()) {
                    int m = metalOf(held[0]);
                    metal += m;
                    if (m == 0) otherItems++;
                }
                ItemStackHandler inv = new ItemStackHandler(2);
                inv.setStackInSlot(0, item);
                inv.setStackInSlot(1, held[0].copy());
                found = SequencedAssemblyRecipe.getRecipe(level, new RecipeWrapper(inv),
                        AllRecipeTypes.DEPLOYING.getType(), DeployerApplicationRecipe.class);
            } else if (step instanceof FillingRecipe) {
                // Same calls the spout makes, with a full tank of the part's molten metal.
                Fluid fluid = BuiltInRegistries.FLUID.get(ResourceLocation.fromNamespaceAndPath(CBC, "molten_" + materialOf(part.get())));
                FluidStack tank = new FluidStack(fluid, 1000);
                int amount = FillingBySpout.getRequiredAmountForItem(level, item, tank);
                if (amount <= 0) return "step " + i + ": spout cannot fill " + item + " with " + tank.getHoverName().getString();
                found = SequencedAssemblyRecipe.getRecipe(level, new SingleRecipeInput(item),
                        AllRecipeTypes.FILLING.getType(), FillingRecipe.class);
                if (found.isEmpty() || !found.get().id().getPath().startsWith(id.getPath()))
                    return "step " + i + " spout picks " + found.map(RecipeHolder::id).orElse(null);
                metal += amount;
                poured += amount;
                item = FillingBySpout.fillItem(level, amount, item, tank.copy());
                continue;
            } else if (step instanceof PressingRecipe) {
                found = SequencedAssemblyRecipe.getRecipe(level, item,
                        AllRecipeTypes.PRESSING.getType(), PressingRecipe.class);
            } else if (step instanceof CuttingRecipe) {
                found = SequencedAssemblyRecipe.getRecipe(level, item,
                        AllRecipeTypes.CUTTING.getType(), CuttingRecipe.class);
            } else {
                return "step " + i + ": unexpected step type " + step.getClass().getSimpleName();
            }
            if (found.isEmpty()) return "step " + i + " (" + step.getClass().getSimpleName() + ") not found for " + item;
            pressed |= step instanceof PressingRecipe;
            sawed |= step instanceof CuttingRecipe;
            if (!found.get().id().getPath().startsWith(id.getPath())) {
                return "step " + i + " picked another recipe: " + found.get().id();
            }
            List<ItemStack> out = found.get().value().rollResults(level.random);
            if (out.isEmpty()) return "step " + i + " produced nothing";
            item = out.get(0);
        }
        if (!ItemStack.isSameItem(item, expected)) return "ended with " + item + " instead of " + expected;
        if (saws < 1 || saws > 2 || (total < 5 && saws != 1)) return saws + " saws in a chain of " + total + " steps";
        if (!bases[0].is(net.minecraft.tags.ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "ingots/" + materialOf(part.get())))))
            return "does not start from an ingot of its material: " + bases[0];

        ResourceLocation finished = ResourceLocation.fromNamespaceAndPath(part.get().modId(), part.get().targetPath());
        if (!BuiltInRegistries.ITEM.getKey(item.getItem()).equals(finished)) {
            // Incomplete part: a deploying recipe must add the one missing item, as in the world.
            String second = runSecondStage(level, item, finished);
            if (second.startsWith("!")) return second.substring(1);
            otherItems++;
        }
        if (part.get().castingIngots() == 0) return null; // helper item, not a cannon part
        int wanted = part.get().castingIngots() * 3 * 90;
        if (metal != wanted) return "uses " + metal + " mB of metal, casting uses " + part.get().castingIngots() * 90 + " mB";
        if (poured == 0 || poured == metal) return "metal must be part ingots, part poured by spout (poured " + poured + " of " + metal + " mB)";
        if (!pressed || !sawed) return "must use the mechanical press and saw";
        if (otherItems > 1) return "consumes " + otherItems + " non-metal items";
        return null;
    }

    private static String materialOf(GeneratedParts.UnfinishedPart part) {
        for (String m : new String[] {"cast_iron", "bronze", "nethersteel", "steel"}) {
            if (part.targetPath().startsWith(m + "_")) return m;
        }
        return "?";
    }

    /** Finds the deploying recipe for the incomplete item the way a deployer does; "!" marks an error. */
    private static String runSecondStage(ServerLevel level, ItemStack incomplete, ResourceLocation finished) {
        for (RecipeHolder<?> holder : level.getRecipeManager().getRecipes()) {
            if (!holder.id().getNamespace().equals(CBCPartsAssembly.MOD_ID)) continue;
            if (!(holder.value() instanceof DeployerApplicationRecipe deploy)) continue;
            if (!deploy.getProcessedItem().test(incomplete)) continue;
            if (deploy.shouldKeepHeldItem()) return "!second stage keeps its held item";
            ItemStack held = deploy.getRequiredHeldItem().getItems()[0].copy();
            if (metalOf(held) != 0) return "!second stage adds metal";
            ItemStackHandler inv = new ItemStackHandler(2);
            inv.setStackInSlot(0, incomplete.copy());
            inv.setStackInSlot(1, held);
            var found = level.getRecipeManager().getRecipeFor(AllRecipeTypes.DEPLOYING.<RecipeWrapper, DeployerApplicationRecipe>getType(),
                    new RecipeWrapper(inv), level);
            if (found.isEmpty() || !found.get().id().equals(holder.id())) return "!deployer picks " + found.map(RecipeHolder::id).orElse(null);
            ItemStack out = deploy.rollResults(level.random).get(0);
            if (!BuiltInRegistries.ITEM.getKey(out.getItem()).equals(finished)) return "!second stage makes " + out;
            return "ok";
        }
        return "!no deploying recipe for " + incomplete;
    }

    /**
     * No two recipes may start the same way, or a machine would pick one at random (by load order).
     * For the first step of every recipe of this mod, every possible base item and held item is
     * tried against all deploying recipes in the game, sequenced (of any mod) and plain; exactly
     * one must match. The same for the second-stage deploying recipes. Also checks that a plain
     * ingot under a press, saw or spout never starts one of this mod's recipes.
     */
    @GameTest(template = "empty")
    public static void noRecipeConflicts(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var manager = level.getRecipeManager();
        List<String> errors = new ArrayList<>();
        int combos = 0;
        for (RecipeHolder<?> holder : manager.getRecipes()) {
            if (!holder.id().getNamespace().equals(CBCPartsAssembly.MOD_ID)) continue;
            ItemStack[] bases;
            ItemStack[] helds;
            if (holder.value() instanceof SequencedAssemblyRecipe recipe) {
                if (!(recipe.getSequence().get(0).getRecipe() instanceof DeployerApplicationRecipe first)) {
                    errors.add(holder.id() + ": does not start with the deployer");
                    continue;
                }
                bases = recipe.getIngredient().getItems();
                helds = first.getRequiredHeldItem().getItems();
            } else if (holder.value() instanceof DeployerApplicationRecipe deploy) {
                bases = deploy.getProcessedItem().getItems();
                helds = deploy.getRequiredHeldItem().getItems();
            } else {
                continue;
            }
            for (ItemStack base : bases) {
                for (ItemStack held : helds) {
                    combos++;
                    ItemStackHandler inv = new ItemStackHandler(2);
                    inv.setStackInSlot(0, base.copy());
                    inv.setStackInSlot(1, held.copy());
                    RecipeWrapper input = new RecipeWrapper(inv);
                    Set<ResourceLocation> matches = new HashSet<>();
                    SequencedAssemblyRecipe.getRecipes(level, base, AllRecipeTypes.DEPLOYING.<RecipeWrapper, DeployerApplicationRecipe>getType(),
                                    DeployerApplicationRecipe.class, r -> r.value().matches(input, level))
                            .forEach(r -> matches.add(r.id()));
                    manager.getRecipesFor(AllRecipeTypes.DEPLOYING.<RecipeWrapper, DeployerApplicationRecipe>getType(), input, level)
                            .forEach(r -> matches.add(r.id()));
                    // a sequenced recipe answers with the id of its step, a plain one with its own id
                    long distinct = matches.stream().map(id -> id.getPath().replaceAll("_step_\\d+$", "")).distinct().count();
                    if (distinct != 1) errors.add(holder.id() + ": " + base + " + " + held + " matches " + matches);
                }
            }
        }
        // A plain ingot under the press, saw or spout must not start this mod's recipes.
        for (String material : new String[] {"cast_iron", "bronze", "steel", "nethersteel"}) {
            var tag = net.minecraft.tags.ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "ingots/" + material));
            for (var entry : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
                ItemStack ingot = new ItemStack(entry.value());
                List<RecipeHolder<?>> started = new ArrayList<>();
                started.addAll(SequencedAssemblyRecipe.getRecipes(level, ingot, AllRecipeTypes.PRESSING.<SingleRecipeInput, PressingRecipe>getType(), PressingRecipe.class, r -> true));
                started.addAll(SequencedAssemblyRecipe.getRecipes(level, ingot, AllRecipeTypes.CUTTING.<RecipeWrapper, CuttingRecipe>getType(), CuttingRecipe.class, r -> true));
                started.addAll(SequencedAssemblyRecipe.getRecipes(level, ingot, AllRecipeTypes.FILLING.<SingleRecipeInput, FillingRecipe>getType(), FillingRecipe.class, r -> true));
                for (RecipeHolder<?> r : started) {
                    if (r.id().getNamespace().equals(CBCPartsAssembly.MOD_ID)) errors.add(ingot + " starts " + r.id() + " without the deployer");
                }
            }
        }
        CBCPartsAssembly.LOGGER.info("[test] recipe starts checked for conflicts: {}", combos);
        if (!errors.isEmpty()) helper.fail(String.join("\n", errors));
        helper.succeed();
    }

    /**
     * Each part with a recipe must also be made in the world: a CBC block recipe (casting or
     * boring) produces it or its incomplete block, and its mould can be crafted. Those block
     * recipes stay in place, the new recipe is only added next to them.
     */
    @GameTest(template = "empty")
    public static void everyRecipePartIsMadeInTheWorld(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Set<ResourceLocation> worldResults = new HashSet<>();
        for (BlockRecipe recipe : BlockRecipesManager.getRecipes()) {
            worldResults.add(BuiltInRegistries.BLOCK.getKey(recipe.getResultBlock()));
        }
        Set<Item> craftable = new HashSet<>();
        for (RecipeHolder<?> holder : level.getRecipeManager().getRecipes()) {
            ItemStack out = holder.value().getResultItem(level.registryAccess());
            if (!out.isEmpty()) craftable.add(out.getItem());
            if (holder.value() instanceof ProcessingRecipe<?, ?> processing) {
                processing.getRollableResultsAsItemStacks().forEach(s -> craftable.add(s.getItem()));
            }
        }
        List<String> errors = new ArrayList<>();
        for (RecipeHolder<?> holder : level.getRecipeManager().getRecipes()) {
            if (!(holder.value() instanceof SequencedAssemblyRecipe recipe)) continue;
            if (!holder.id().getNamespace().equals(CBCPartsAssembly.MOD_ID)) continue;
            ResourceLocation part = BuiltInRegistries.ITEM.getKey(recipe.getResultItem(level.registryAccess()).getItem());
            if (partFor(part).map(p -> p.castingIngots() == 0).orElse(false)) continue; // helper item
            ResourceLocation incomplete = ResourceLocation.fromNamespaceAndPath(part.getNamespace(), "incomplete_" + part.getPath());
            if (!worldResults.contains(part) && !worldResults.contains(incomplete)) {
                errors.add(part + ": no casting/boring recipe in the world");
            }
            SequencedRecipe<?> first = recipe.getSequence().get(0);
            if (first.getRecipe() instanceof DeployerApplicationRecipe deploy && deploy.shouldKeepHeldItem()) {
                Item mould = deploy.getRequiredHeldItem().getItems()[0].getItem();
                if (!craftable.contains(mould)) errors.add(part + ": its mould " + BuiltInRegistries.ITEM.getKey(mould) + " cannot be crafted");
            } else {
                errors.add(part + ": first step is not the mould");
            }
        }
        // the item a second-stage recipe fits must be obtainable too
        for (RecipeHolder<?> holder : level.getRecipeManager().getRecipes()) {
            if (!holder.id().getNamespace().equals(CBCPartsAssembly.MOD_ID)) continue;
            if (!(holder.value() instanceof DeployerApplicationRecipe deploy)) continue;
            Item component = deploy.getRequiredHeldItem().getItems()[0].getItem();
            if (!craftable.contains(component)) errors.add(holder.id() + ": " + BuiltInRegistries.ITEM.getKey(component) + " cannot be crafted");
        }
        if (!errors.isEmpty()) helper.fail(String.join("\n", errors));
        helper.succeed();
    }

    /**
     * Looks for gaps: every part of a non-big cannon family that the world makes by casting and
     * boring (CBC block recipes of all installed mods) must have a recipe of this mod.
     */
    @GameTest(template = "empty")
    public static void everyWorldMadePartHasRecipe(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<Class<?>> families = new ArrayList<>();
        for (String name : io.github.iksanbro.cbcparts.CannonPartConnector.FAMILY_INTERFACES) {
            try {
                families.add(Class.forName(name, false, CannonPartsGameTests.class.getClassLoader()));
            } catch (ClassNotFoundException e) {
                // mod not installed
            }
        }
        Set<ResourceLocation> covered = new HashSet<>();
        for (RecipeHolder<?> holder : level.getRecipeManager().getRecipes()) {
            if (!holder.id().getNamespace().equals(CBCPartsAssembly.MOD_ID)) continue;
            ResourceLocation out = BuiltInRegistries.ITEM.getKey(holder.value().getResultItem(level.registryAccess()).getItem());
            covered.add(out);
        }
        Set<ResourceLocation> worldParts = new java.util.TreeSet<>();
        for (BlockRecipe recipe : BlockRecipesManager.getRecipes()) {
            ResourceLocation result = BuiltInRegistries.BLOCK.getKey(recipe.getResultBlock());
            if (result.getPath().startsWith("unbored_")) continue; // casting; the part comes from boring
            ResourceLocation part = ResourceLocation.fromNamespaceAndPath(result.getNamespace(),
                    result.getPath().replaceFirst("^incomplete_", ""));
            var block = BuiltInRegistries.BLOCK.get(part);
            if (families.stream().anyMatch(c -> c.isInstance(block))) worldParts.add(part);
        }
        List<ResourceLocation> missing = worldParts.stream().filter(p -> !covered.contains(p)).toList();
        CBCPartsAssembly.LOGGER.info("[test] world-made non-big parts: {}, without recipe: {}", worldParts.size(), missing);
        if (!missing.isEmpty()) helper.fail("world-made parts without a recipe: " + missing);
        if (worldParts.size() != (int) loadedParts().stream().filter(p -> p.castingIngots() > 0).count())
            helper.fail("world-made parts " + worldParts.size() + " != parts with recipe");
        helper.succeed();
    }

    // ---------------------------------------------------------------------------------
    // Survival assembly

    @GameTest(template = "empty")
    public static void cbcAutocannonAssemblesInSurvival(GameTestHelper helper) {
        assembleOnMount(helper, CBC, 1, true,
                "steel_autocannon_breech", "steel_autocannon_recoil_spring",
                "steel_autocannon_barrel", "steel_autocannon_barrel");
    }

    @GameTest(template = "empty")
    public static void cbcBronzeAutocannonAssemblesInSurvival(GameTestHelper helper) {
        assembleOnMount(helper, CBC, 1, true,
                "bronze_autocannon_breech", "bronze_autocannon_recoil_spring", "bronze_autocannon_barrel");
    }

    /** Control: with the option turned off CBC refuses to assemble survival-placed parts. */
    @GameTest(template = "empty")
    public static void autocannonFailsWhenConnectionDisabled(GameTestHelper helper) {
        Config.CONNECT_IN_SURVIVAL.set(false);
        try {
            assembleOnMount(helper, CBC, 1, false,
                    "cast_iron_autocannon_breech", "cast_iron_autocannon_recoil_spring", "cast_iron_autocannon_barrel");
        } finally {
            Config.CONNECT_IN_SURVIVAL.set(true);
        }
    }

    @GameTest(template = "empty")
    public static void mediumCannonAssemblesInSurvival(GameTestHelper helper) {
        if (skipWithout(helper, NW)) return;
        assembleOnMount(helper, NW, 1, true,
                "nethersteel_mediumcannon_breech", "nethersteel_mediumcannon_recoil_barrel",
                "nethersteel_mediumcannon_barrel", "nethersteel_mediumcannon_barrel");
    }

    @GameTest(template = "empty")
    public static void rotaryCannonAssemblesInSurvival(GameTestHelper helper) {
        if (skipWithout(helper, NW)) return;
        assembleOnMount(helper, NW, 1, true,
                "steel_rotarycannon_breech", "steel_rotarycannon_bearing",
                "steel_rotarycannon_barrel", "steel_rotarycannon_barrel");
    }

    @GameTest(template = "empty")
    public static void twinAutocannonAssemblesInSurvival(GameTestHelper helper) {
        if (skipWithout(helper, CBCAT)) return;
        assembleOnMount(helper, CBCAT, 1, true,
                "steel_twin_autocannon_breech", "steel_twin_autocannon_recoil_spring",
                "steel_twin_autocannon_barrel", "steel_twin_autocannon_muzzle_brake");
    }

    @GameTest(template = "empty")
    public static void heavyAutocannonAssemblesInSurvival(GameTestHelper helper) {
        if (skipWithout(helper, CBCAT)) return;
        assembleOnMount(helper, CBCAT, 1, true,
                "steel_heavy_autocannon_breech", "steel_heavy_autocannon_recoil_spring",
                "steel_heavy_autocannon_barrel", "steel_heavy_autocannon_silencer");
    }

    @GameTest(template = "empty")
    public static void rocketPodAssemblesInSurvival(GameTestHelper helper) {
        if (skipWithout(helper, CBCAT)) return;
        assembleOnMount(helper, CBCAT, 0, true, "steel_rocket_pod_breech", "steel_rocket_pod_rail");
    }

    private static boolean skipWithout(GameTestHelper helper, String mod) {
        if (ModList.get().isLoaded(mod)) return false;
        helper.succeed();
        return true;
    }

    /**
     * Puts a CBC cannon mount down, places the given parts one by one as a survival player
     * (breech first, along +Z) so that the part at {@code mountIndex} sits two blocks above the
     * mount, then asks the mount to assemble.
     */
    private static void assembleOnMount(GameTestHelper helper, String mod, int mountIndex, boolean expectSuccess,
                                        String... parts) {
        for (int x = 0; x < 12; x++)
            for (int z = 0; z < 12; z++)
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE.defaultBlockState());

        BlockPos mountRel = new BlockPos(5, 1, 5);
        BlockState mount = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(CBC, "cannon_mount"))
                .defaultBlockState();
        if (mount.hasProperty(BlockStateProperties.HORIZONTAL_FACING))
            mount = mount.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
        // VERTICAL_DIRECTION=DOWN is a mount standing on the floor; the cannon goes two blocks above it.
        if (mount.hasProperty(BlockStateProperties.VERTICAL_DIRECTION))
            mount = mount.setValue(BlockStateProperties.VERTICAL_DIRECTION, Direction.DOWN);
        helper.setBlock(mountRel, mount);

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setYRot(180f); // looking north, so parts face south (+Z)
        player.setXRot(0f);

        List<BlockPos> placed = new ArrayList<>();
        for (int i = 0; i < parts.length; i++) {
            BlockPos rel = mountRel.above(2).relative(Direction.SOUTH, i - mountIndex);
            BlockPos abs = helper.absolutePos(rel);
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(mod, parts[i]));
            if (item == Items.AIR) helper.fail("unknown item " + mod + ":" + parts[i]);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
            player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
            if (helper.getLevel().getBlockState(abs).isAir()) helper.fail("could not place " + parts[i] + " at " + rel);
            placed.add(abs);
        }

        String connections = describeConnections(helper.getLevel(), placed);
        BlockEntity mountBe = helper.getBlockEntity(mountRel);
        String error = null;
        boolean assembled;
        try {
            Method assemble = findMethod(mountBe.getClass(), "assemble");
            assemble.setAccessible(true);
            assemble.invoke(mountBe);
            Method getContraption = findMethod(mountBe.getClass(), "getContraption");
            assembled = getContraption.invoke(mountBe) != null;
        } catch (java.lang.reflect.InvocationTargetException e) {
            assembled = false;
            error = String.valueOf(e.getCause());
        } catch (ReflectiveOperationException e) {
            helper.fail("cannot call the cannon mount: " + e);
            return;
        }
        CBCPartsAssembly.LOGGER.info("[test] {} {}: assembled={} error={} connections={}",
                mod, String.join(",", parts), assembled, error, connections);
        if (assembled != expectSuccess) {
            helper.fail((expectSuccess ? "cannon did not assemble: " + error : "cannon assembled unexpectedly")
                    + " | connections: " + connections);
        }
        helper.succeed();
    }

    private static Method findMethod(Class<?> cls, String name) throws NoSuchMethodException {
        for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                if (m.getName().equals(name) && m.getParameterCount() == 0) return m;
            }
        }
        throw new NoSuchMethodException(cls.getName() + "." + name);
    }

    private static String describeConnections(ServerLevel level, List<BlockPos> placed) {
        StringBuilder sb = new StringBuilder();
        for (BlockPos pos : placed) {
            BlockEntity be = level.getBlockEntity(pos);
            sb.append(BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).getPath()).append('[');
            if (be instanceof ICannonBlockEntity<?> cannon) {
                sb.append("N=").append(cannon.cannonBehavior().isConnectedTo(Direction.NORTH))
                        .append(" S=").append(cannon.cannonBehavior().isConnectedTo(Direction.SOUTH));
            } else {
                sb.append("no cannon BE");
            }
            sb.append("] ");
        }
        return sb.toString().trim();
    }
}
