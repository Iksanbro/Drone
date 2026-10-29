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
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
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
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;
import rbasamoyai.createbigcannons.cannons.ICannonBlockEntity;

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
     * different recipe answers (ambiguous start), or if the end result is wrong.
     */
    @GameTest(template = "empty")
    public static void everyRecipeBuildsItsPart(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<String> errors = new ArrayList<>();
        int checked = 0;
        for (RecipeHolder<?> holder : level.getRecipeManager().getRecipes()) {
            if (!holder.id().getNamespace().equals(CBCPartsAssembly.MOD_ID)) continue;
            if (!(holder.value() instanceof SequencedAssemblyRecipe recipe)) continue;
            checked++;
            String problem = runRecipe(level, holder.id(), recipe);
            if (problem != null) errors.add(holder.id() + ": " + problem);
        }
        int expected = expectedSequencedRecipes();
        CBCPartsAssembly.LOGGER.info("[test] sequenced recipes checked: {} (expected {})", checked, expected);
        if (checked != expected) errors.add("found " + checked + " sequenced recipes, expected " + expected);
        if (!errors.isEmpty()) helper.fail(String.join("\n", errors));
        helper.succeed();
    }

    private static int expectedSequencedRecipes() {
        ModList mods = ModList.get();
        return (int) GeneratedParts.ALL.stream().filter(p -> mods.isLoaded(p.modId())).count();
    }

    private static String runRecipe(ServerLevel level, ResourceLocation id, SequencedAssemblyRecipe recipe) {
        ItemStack[] bases = recipe.getIngredient().getItems();
        if (bases.length == 0) return "base ingredient has no items";
        ItemStack item = bases[0].copy();
        ItemStack expected = recipe.getResultItem(level.registryAccess());
        List<SequencedRecipe<?>> sequence = recipe.getSequence();
        int total = recipe.getLoops() * sequence.size();
        for (int i = 0; i < total; i++) {
            ProcessingRecipe<?, ?> step = sequence.get(i % sequence.size()).getRecipe();
            Optional<? extends RecipeHolder<? extends ProcessingRecipe<?, ?>>> found;
            if (step instanceof DeployerApplicationRecipe deploy) {
                ItemStack[] held = deploy.getRequiredHeldItem().getItems();
                if (held.length == 0) return "step " + i + ": held item ingredient is empty";
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
        return null;
    }

    /** Neo Warfare has no recipes for its moulds and components in 1.21.1; ours must be loaded. */
    @GameTest(template = "empty")
    public static void neoWarfareHelperRecipesLoaded(GameTestHelper helper) {
        if (!ModList.get().isLoaded(NW)) {
            helper.succeed();
            return;
        }
        var recipes = helper.getLevel().getRecipeManager();
        String[] ids = {
                "cutting/cbcmodernwarfare/mediumcannon_barrel_cast_mould",
                "cutting/cbcmodernwarfare/rotarycannon_bearing_cast_mould",
                "sequenced_assembly/cbcmodernwarfare/components/steel_mediumcannon_falling_breech",
                "sequenced_assembly/cbcmodernwarfare/components/steel_rotarycannon_breech_extractor",
                "sequenced_assembly/cbcmodernwarfare/components/heavy_recoil_spring",
        };
        for (String path : ids) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(CBCPartsAssembly.MOD_ID, path);
            helper.assertTrue(recipes.byKey(id).isPresent(), "missing recipe " + id);
        }
        helper.succeed();
    }

    /** Three CBC:AT mould recipes fail to parse in 0.1.4c; ours replace them. */
    @GameTest(template = "empty")
    public static void brokenAdvancedTechnologiesMouldsCraftable(GameTestHelper helper) {
        if (skipWithout(helper, CBCAT)) return;
        var recipes = helper.getLevel().getRecipeManager();
        for (String mould : new String[] {"twin_autocannon_barrel_mould", "rocket_pod_rail_mould", "rocket_pod_breech_mould"}) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(CBCPartsAssembly.MOD_ID, "cutting/cbc_at/" + mould);
            helper.assertTrue(recipes.byKey(id).isPresent(), "missing recipe " + id);
        }
        helper.succeed();
    }

    // ---------------------------------------------------------------------------------
    // Loot tables

    /** Addon loot tables sit at the pre-1.21 path, so parts would drop nothing. Ours fix that. */
    @GameTest(template = "empty")
    public static void addonPartsHaveLootTables(GameTestHelper helper) {
        var registries = helper.getLevel().getServer().reloadableRegistries();
        List<String> missing = new ArrayList<>();
        for (GeneratedParts.UnfinishedPart part : GeneratedParts.ALL) {
            if (!ModList.get().isLoaded(part.modId()) || part.modId().equals(CBC)) continue;
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(part.modId(), part.targetPath()));
            if (!(item instanceof net.minecraft.world.item.BlockItem)) continue; // components are plain items
            ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE,
                    ResourceLocation.fromNamespaceAndPath(part.modId(), "blocks/" + part.targetPath()));
            if (registries.getLootTable(key) == LootTable.EMPTY) missing.add(key.location().toString());
        }
        if (!missing.isEmpty()) helper.fail("missing loot tables: " + missing);

        // Blocks this mod does not cover show the original problem: their tables are not loaded.
        String[][] uncovered = {{CBCAT, "unbored_steel_twin_autocannon_barrel"}, {NW, "unbored_steel_mediumcannon_barrel"}};
        for (String[] block : uncovered) {
            if (!ModList.get().isLoaded(block[0])) continue;
            ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE,
                    ResourceLocation.fromNamespaceAndPath(block[0], "blocks/" + block[1]));
            CBCPartsAssembly.LOGGER.info("[test] original loot table {} loaded: {}", key.location(),
                    registries.getLootTable(key) != LootTable.EMPTY);
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
