package io.github.iksanbro.cbcparts.test;

import com.simibubi.create.AllRecipeTypes;
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
import net.minecraft.world.level.GameType;
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
     * does not use exactly 3x the metal of the casting and at most one other consumed item.
     */
    @GameTest(template = "empty")
    public static void everyRecipeBuildsItsPartForTripleMetal(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<String> errors = new ArrayList<>();
        int checked = 0;
        for (RecipeHolder<?> holder : level.getRecipeManager().getRecipes()) {
            if (!holder.id().getNamespace().equals(CBCPartsAssembly.MOD_ID)) continue;
            if (!(holder.value() instanceof SequencedAssemblyRecipe recipe)) {
                errors.add(holder.id() + ": the mod must only add sequenced assembly recipes");
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
        return loadedParts().stream()
                .filter(p -> p.modId().equals(result.getNamespace()) && p.targetPath().equals(result.getPath()))
                .findFirst();
    }

    /** Ingots of metal in one item: 1 for an ingot, 9 for a storage block, 0 otherwise. */
    private static int metalOf(ItemStack stack) {
        return stack.getTags().mapToInt(tag -> {
            if (!tag.location().getNamespace().equals("c")) return 0;
            String path = tag.location().getPath();
            if (path.startsWith("ingots/")) return 1;
            if (path.startsWith("storage_blocks/")) return 9;
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
        int otherItems = metal == 0 ? 1 : 0;
        List<SequencedRecipe<?>> sequence = recipe.getSequence();
        int total = recipe.getLoops() * sequence.size();
        for (int i = 0; i < total; i++) {
            ProcessingRecipe<?, ?> step = sequence.get(i % sequence.size()).getRecipe();
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
            if (!found.get().id().getPath().startsWith(id.getPath())) {
                return "step " + i + " picked another recipe: " + found.get().id();
            }
            List<ItemStack> out = found.get().value().rollResults(level.random);
            if (out.isEmpty()) return "step " + i + " produced nothing";
            item = out.get(0);
        }
        if (!ItemStack.isSameItem(item, expected)) return "ended with " + item + " instead of " + expected;
        int wanted = part.get().castingIngots() * 3;
        if (metal != wanted) return "uses " + metal + " ingots of metal, casting uses " + part.get().castingIngots();
        if (otherItems > 1) return "consumes " + otherItems + " non-metal items";
        return null;
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
        if (!errors.isEmpty()) helper.fail(String.join("\n", errors));
        helper.succeed();
    }

    /** Parts whose world process is broken or missing get nothing. */
    @GameTest(template = "empty")
    public static void brokenPartsGetNoRecipe(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<String> forbidden = List.of("rotarycannon", "twin_autocannon_barrel", "rocket_pod");
        for (RecipeHolder<?> holder : level.getRecipeManager().getRecipes()) {
            if (!holder.id().getNamespace().equals(CBCPartsAssembly.MOD_ID)) continue;
            String path = BuiltInRegistries.ITEM.getKey(holder.value().getResultItem(level.registryAccess()).getItem()).getPath();
            for (String f : forbidden) {
                if (path.contains(f) && !path.contains("vert_twin")) helper.fail("unexpected recipe " + holder.id());
            }
        }
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
