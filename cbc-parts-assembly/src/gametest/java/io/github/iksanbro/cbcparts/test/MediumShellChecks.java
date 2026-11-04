package io.github.iksanbro.cbcparts.test;

import com.simibubi.create.content.fluids.transfer.GenericItemFilling;
import io.github.iksanbro.cbcparts.CBCPartsAssembly;
import io.github.iksanbro.cbcparts.medium.FluidMediumRoundItem;
import io.github.iksanbro.cbcparts.medium.MediumBunkerBusterProjectile;
import io.github.iksanbro.cbcparts.medium.MediumCartridgeItem;
import io.github.iksanbro.cbcparts.medium.MediumClusterProjectile;
import io.github.iksanbro.cbcparts.medium.MediumFluidProjectile;
import io.github.iksanbro.cbcparts.medium.MediumRoundItem;
import io.github.iksanbro.cbcparts.medium.MediumShell;
import io.github.iksanbro.cbcparts.medium.MediumShells;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import rbasamoyai.createbigcannons.cannon_control.contraption.PitchOrientedContraptionEntity;
import riftyboi.cbcmodernwarfare.cannon_control.contraption.MountedMediumcannonContraption;
import riftyboi.cbcmodernwarfare.cannons.medium_cannon.breech.MediumcannonBreechBlockEntity;
import net.minecraft.core.Position;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import rbasamoyai.createbigcannons.munitions.AbstractCannonProjectile;
import rbasamoyai.createbigcannons.munitions.big_cannon.fluid_shell.FluidBlobBurst;
import rbasamoyai.ritchiesprojectilelib.projectile_burst.ProjectileBurst;
import riftyboi.cbcmodernwarfare.munitions.ItemStackDataUtil;
import riftyboi.cbcmodernwarfare.munitions.medium_cannon.AbstractMediumcannonProjectile;
import riftyboi.cbcmodernwarfare.munitions.medium_cannon.MediumcannonCartridgeItem;
import riftyboi.cbcmodernwarfare.munitions.medium_cannon.MediumcannonFuzedProjectile;

/**
 * Checks of {@link MediumShellGameTests}, in a class of their own: it refers to CBC Neo Warfare and is
 * loaded only when that mod is installed.
 *
 * <p>Medium cannon rounds made after big shells (needs CBC Neo Warfare; the Mk rounds need CBC
 * Enchanced Shells, run with -PwithEnhancedShells). Things that explode are set off 60 blocks above
 * the test, away from the other tests.
 */
final class MediumShellChecks {
    private MediumShellChecks() {}
    private static final String NW = "cbcmodernwarfare";
    private static final String ES = MediumShell.ENHANCED_SHELLS;

    private static EntityType<?> type(String id) {
        return BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(id));
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
    }

    private static <T extends Entity> T spawn(GameTestHelper helper, String type, Vec3 relative, Vec3 velocity) {
        ServerLevel level = helper.getLevel();
        @SuppressWarnings("unchecked")
        T e = (T) type(type).create(level);
        Vec3 at = helper.absoluteVec(relative);
        e.setPos(at.x, at.y, at.z);
        e.setDeltaMovement(velocity);
        if (e instanceof AbstractCannonProjectile p && velocity.lengthSqr() > 0) p.setOrientation(velocity.normalize());
        level.addFreshEntity(e);
        return e;
    }

    private static void detonate(MediumcannonFuzedProjectile projectile) throws Exception {
        Method m = null;
        for (Class<?> c = projectile.getClass(); m == null && c != null; c = c.getSuperclass()) {
            try {
                m = c.getDeclaredMethod("detonate", Position.class);
            } catch (NoSuchMethodException ignored) {
                // look in the superclass
            }
        }
        m.setAccessible(true);
        m.invoke(projectile, projectile.position());
        projectile.discard();
    }

    private static <T extends Entity> List<T> around(GameTestHelper helper, EntityType<T> type, Vec3 relative, double radius) {
        return helper.getLevel().getEntities(type, new AABB(helper.absoluteVec(relative), helper.absoluteVec(relative)).inflate(radius), e -> true);
    }

    /** Every round, cartridge and projectile is there and its munition properties loaded. */
    public static void everyRoundIsRegistered(GameTestHelper helper) {
        List<MediumShells.Entry> entries = MediumShells.entries();
        int expected = MediumShell.createBigCannons().size() + (ModList.get().isLoaded(ES) ? MediumShell.enhancedShells().size() : 0);
        helper.assertTrue(entries.size() == expected, "expected " + expected + " medium shells, got " + entries.size());
        for (MediumShells.Entry e : entries) {
            String name = e.shell().name();
            MediumRoundItem round = e.round().get();
            helper.assertTrue(round.getCartridgeType().getItem() == e.cartridge().get(), name + ": cartridge of the round");
            Entity entity = e.entity().get().create(helper.getLevel());
            helper.assertTrue(entity instanceof AbstractMediumcannonProjectile, name + ": medium cannon projectile");
            AbstractMediumcannonProjectile projectile = (AbstractMediumcannonProjectile) entity;
            helper.assertTrue(projectile.getItem().is(round), name + ": the projectile shows its round");
            helper.assertTrue(round.getMediumcannonProperties(ItemStack.EMPTY).addedRecoil() > 0,
                    name + ": munition properties not loaded (medium_" + name + ".json)");
            helper.assertTrue(e.cartridge().get().filled().getItem() instanceof MediumcannonCartridgeItem, name + ": cartridge");
            ItemStack filled = e.cartridge().get().filled();
            Entity fired = ((MediumcannonCartridgeItem) filled.getItem()).getMediumcannonProjectile(filled, helper.getLevel());
            helper.assertTrue(fired != null && fired.getType() == e.entity().get(), name + ": the cartridge fires its round");
        }
        CBCPartsAssembly.LOGGER.info("[test] {} medium shells registered", entries.size());
        helper.succeed();
    }

    /** Half the damage of the big shell. */
    public static void halfOfTheBigShell(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        String[][] pairs = {
                {"shrapnel_shell", "createbigcannons:shrapnel_shell"}, {"fluid_shell", "createbigcannons:fluid_shell"},
                {"solid_shot", "createbigcannons:shot"}, {"drop_mortar_shell", "createbigcannons:drop_mortar_shell"},
                {"mortar_stone", "createbigcannons:mortar_stone"},
        };
        java.util.List<String[]> all = new java.util.ArrayList<>(List.of(pairs));
        if (ModList.get().isLoaded(ES)) {
            // the server's tuned files are not in the test world: compare the shells left at the mod's defaults
            for (String t : List.of("cluster_shell", "bunker_buster_shell", "mortar_stone"))
                for (int mk = 1; mk <= 5; mk++) all.add(new String[]{t + "_mk" + mk, ES + ":" + t + "_mk" + mk});
        }
        for (String[] p : all) {
            AbstractCannonProjectile medium = (AbstractCannonProjectile) type("cbcparts:medium_" + p[0]).create(level);
            AbstractCannonProjectile big = (AbstractCannonProjectile) type(p[1]).create(level);
            float m = medium.getDamageProperties().entityDamage(), b = big.getDamageProperties().entityDamage();
            helper.assertTrue(Math.abs(m - b / 2) < 1e-3, p[0] + ": damage " + m + ", big shell " + b);
        }
        if (ModList.get().isLoaded(ES)) {
            // tuned values: HE shell Mk.I 90 damage, 20/16 explosion -> 45
            AbstractCannonProjectile he = (AbstractCannonProjectile) type("cbcparts:medium_he_shell_mk1").create(level);
            helper.assertTrue(he.getDamageProperties().entityDamage() == 45, "HE Mk.I damage " + he.getDamageProperties().entityDamage());
        }
        helper.succeed();
    }

    /** Shrapnel round: half as many fragments as the big shell (50 -> 25), of the medium kind. */
    public static void shrapnelRoundBursts(GameTestHelper helper) throws Exception {
        Vec3 at = new Vec3(6, 64, 6);
        MediumcannonFuzedProjectile shell = spawn(helper, "cbcparts:medium_shrapnel_shell", at, new Vec3(0.5, 0, 0));
        detonate(shell);
        List<? extends ProjectileBurst> bursts = around(helper, MediumShells.SHRAPNEL_BURST.get(), at, 4);
        helper.assertTrue(bursts.size() == 1, "one burst of shrapnel, got " + bursts.size());
        helper.assertTrue(bursts.getFirst().getSubProjectileCount() == 25, "fragments: " + bursts.getFirst().getSubProjectileCount());
        bursts.forEach(Entity::discard);
        helper.succeed();
    }

    /** Fluid round: a spout fills one round (also from a stack) with up to 1000 mB; it splashes them. */
    public static void fluidRoundIsFilledAndSplashes(GameTestHelper helper) throws Exception {
        ServerLevel level = helper.getLevel();
        ItemStack stack = new ItemStack(item("cbcparts:medium_fluid_shell_round"), 2);
        FluidStack water = new FluidStack(Fluids.WATER, 2000);
        helper.assertTrue(GenericItemFilling.canItemBeFilled(level, stack), "spout can fill a stack of rounds");
        int needed = GenericItemFilling.getRequiredAmountForItem(level, stack, water.copy());
        helper.assertTrue(needed == 1000, "spout pours " + needed + " mB");
        ItemStack filled = GenericItemFilling.fillItem(level, needed, stack, water.copy());
        helper.assertTrue(stack.getCount() == 1, "one round taken off the stack");
        helper.assertTrue(FluidMediumRoundItem.fluid(filled).getAmount() == 1000, "filled: " + FluidMediumRoundItem.fluid(filled).getAmount());
        IFluidHandlerItem tank = filled.getCapability(Capabilities.FluidHandler.ITEM);
        helper.assertTrue(tank.fill(new FluidStack(Fluids.LAVA, 100), IFluidHandler.FluidAction.SIMULATE) == 0, "full round takes no more");

        MediumFluidProjectile projectile = (MediumFluidProjectile) ((MediumRoundItem) filled.getItem()).getMediumcannonProjectile(filled, level);
        helper.assertTrue(projectile.getFluid().getAmount() == 1000, "projectile carries the fluid");
        Vec3 at = new Vec3(6, 64, 6);
        Vec3 abs = helper.absoluteVec(at);
        projectile.setPos(abs.x, abs.y, abs.z);
        projectile.setDeltaMovement(0.5, 0, 0);
        level.addFreshEntity(projectile);
        detonate(projectile);
        List<FluidBlobBurst> blobs = around(helper, rbasamoyai.createbigcannons.index.CBCEntityTypes.FLUID_BLOB_BURST.get(), at, 4);
        helper.assertTrue(blobs.size() == 1 && blobs.getFirst().getSubProjectileCount() == 2, "two blobs of 500 mB");
        helper.assertTrue(blobs.getFirst().getFluidStack().fluid() == Fluids.WATER, "blobs of water");
        blobs.forEach(Entity::discard);
        helper.succeed();
    }

    /** Mortar stone round explodes where it hits a block. */
    public static void mortarStoneRoundExplodesOnImpact(GameTestHelper helper) {
        BlockPos target = new BlockPos(6, 60, 6);
        helper.setBlock(target, Blocks.DIRT);
        Entity stone = spawn(helper, "cbcparts:medium_mortar_stone", new Vec3(6.5, 66, 6.5), new Vec3(0, -1, 0));
        helper.succeedWhen(() -> {
            helper.assertTrue(stone.isRemoved(), "still flying");
            helper.assertBlockNotPresent(Blocks.DIRT, target);
        });
    }

    /** Neo Warfare's crafting recipes take the rounds: fuzing and cartridge assembly; and this mod's recipes. */
    public static void roundsGoIntoCartridges(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ItemStack round = new ItemStack(item("cbcparts:medium_shrapnel_shell_round"));
        ItemStack fuze = new ItemStack(item("createbigcannons:impact_fuze"));
        ItemStack fuzed = craft(helper, CraftingInput.of(1, 2, List.of(fuze, round)));
        helper.assertTrue(fuzed.is(round.getItem()) && ItemStackDataUtil.hasFuze(fuzed), "fuzed round");

        ItemStack cartridgeIn = new ItemStack(item("cbcmodernwarfare:filled_mediumcannon_cartridge"));
        ItemStack cartridge = craft(helper, CraftingInput.of(1, 2, List.of(fuzed, cartridgeIn)));
        helper.assertTrue(cartridge.getItem() == item("cbcparts:medium_shrapnel_shell_cartridge"), "cartridge: " + cartridge);
        ItemStack inside = MediumcannonCartridgeItem.getProjectileStack(cartridge);
        helper.assertTrue(inside.is(round.getItem()) && ItemStackDataUtil.hasFuze(inside), "fuzed round inside");
        Entity fired = ((MediumCartridgeItem) cartridge.getItem()).getMediumcannonProjectile(cartridge, level);
        helper.assertTrue(fired != null && fired.getType() == type("cbcparts:medium_shrapnel_shell"), "fires a shrapnel round");

        var recipes = level.getRecipeManager();
        Optional<RecipeHolder<?>> split = recipes.byKey(ResourceLocation.parse("cbcparts:medium_shells/medium_shrapnel_shell_round"));
        helper.assertTrue(split.isPresent(), "big shell -> rounds recipe");
        ItemStack two = split.get().value().getResultItem(level.registryAccess());
        helper.assertTrue(two.is(round.getItem()) && two.getCount() == 2, "two rounds from a big shell: " + two);
        Optional<RecipeHolder<?>> deploy = recipes.byKey(ResourceLocation.parse("cbcparts:medium_shells/deploying/medium_shrapnel_shell_cartridge"));
        helper.assertTrue(deploy.isPresent(), "deployer recipe");
        ItemStack deployed = deploy.get().value().getResultItem(level.registryAccess());
        helper.assertTrue(MediumcannonCartridgeItem.getProjectileStack(deployed).is(round.getItem()), "deployer puts the round in: " + deployed);
        helper.succeed();
    }

    private static ItemStack craft(GameTestHelper helper, CraftingInput input) {
        ServerLevel level = helper.getLevel();
        Optional<RecipeHolder<CraftingRecipe>> recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
        helper.assertTrue(recipe.isPresent(), "no crafting recipe for " + input.items());
        return recipe.get().value().assemble(input, level.registryAccess());
    }

    /** Grapeshot round bursts at the muzzle into half as many shots as the big bag (75 -> 38). */
    public static void grapeshotRoundBursts(GameTestHelper helper) {
        Vec3 at = new Vec3(6, 64, 6);
        Entity shot = spawn(helper, "cbcparts:medium_bag_of_grapeshot_mk1", at, new Vec3(0.5, 0, 0));
        helper.succeedWhen(() -> {
            helper.assertTrue(shot.isRemoved(), "not burst yet");
            List<? extends ProjectileBurst> bursts = around(helper, MediumShells.GRAPESHOT_BURST.get(), at, 6);
            helper.assertTrue(bursts.size() == 1 && bursts.getFirst().getSubProjectileCount() == 38,
                    "grapeshot: " + bursts.stream().map(ProjectileBurst::getSubProjectileCount).toList());
            bursts.forEach(Entity::discard);
        });
    }

    /** Cluster round Mk.III splits at the top of its flight into 6 medium HE rounds Mk.II (big: 12 HE shells). */
    public static void clusterRoundSplits(GameTestHelper helper) {
        Vec3 at = new Vec3(6, 64, 6);
        MediumClusterProjectile cluster = spawn(helper, "cbcparts:medium_cluster_shell_mk3", at, new Vec3(0, 1.2, 0));
        EntityType<?> sub = type("cbcparts:medium_he_shell_mk2");
        helper.succeedWhen(() -> {
            helper.assertTrue(cluster.hasSplit() && cluster.isRemoved(), "not split yet");
            List<? extends Entity> subs = helper.getLevel().getEntities(sub, cluster.getBoundingBox().inflate(16), e -> true);
            helper.assertTrue(subs.size() == 6, "sub-rounds: " + subs.size());
            subs.forEach(Entity::discard); // they would come down on the other tests
        });
    }

    /** Bunker buster Mk.I drills 5 blocks (big: 10), then explodes. */
    public static void bunkerBusterDrills(GameTestHelper helper) {
        for (int x = 3; x < 12; x++) helper.setBlock(new BlockPos(x, 64, 6), Blocks.STONE);
        MediumBunkerBusterProjectile shell = spawn(helper, "cbcparts:medium_bunker_buster_shell_mk1",
                new Vec3(0.5, 64.5, 6.5), new Vec3(1.5, 0, 0));
        boolean[] drilled = {false};
        helper.onEachTick(() -> drilled[0] |= shell.isDrilling());
        helper.succeedWhen(() -> {
            helper.assertTrue(drilled[0], "never started drilling");
            helper.assertTrue(shell.isRemoved(), "still drilling");
            helper.assertTrue(shell.drilledBlocks() == 5, "drilled " + shell.drilledBlocks());
        });
    }

    /**
     * A medium cannon built in survival on a cannon mount, loaded with a shrapnel cartridge, fires
     * (Neo Warfare's own firing code): the shrapnel round flies out and the breech holds the spent case.
     */
    public static void mediumCannonFiresTheRound(GameTestHelper helper) throws Exception {
        ServerLevel level = helper.getLevel();
        for (int x = 0; x < 12; x++)
            for (int z = 0; z < 12; z++) helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        BlockPos mountRel = new BlockPos(5, 1, 2);
        BlockState mount = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("createbigcannons:cannon_mount")).defaultBlockState();
        if (mount.hasProperty(BlockStateProperties.HORIZONTAL_FACING))
            mount = mount.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
        if (mount.hasProperty(BlockStateProperties.VERTICAL_DIRECTION))
            mount = mount.setValue(BlockStateProperties.VERTICAL_DIRECTION, Direction.DOWN);
        helper.setBlock(mountRel, mount);

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setYRot(180f); // parts face south (+Z)
        String[] parts = {"steel_mediumcannon_breech", "steel_mediumcannon_recoil_barrel",
                "steel_mediumcannon_barrel", "steel_mediumcannon_barrel"};
        BlockPos breechAbs = null;
        for (int i = 0; i < parts.length; i++) {
            BlockPos abs = helper.absolutePos(mountRel.above(2).relative(Direction.SOUTH, i - 1));
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(NW + ":" + parts[i])));
            player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false)));
            if (i == 0) breechAbs = abs;
        }
        MediumcannonBreechBlockEntity breech = (MediumcannonBreechBlockEntity) level.getBlockEntity(breechAbs);
        breech.setInputBuffer(((MediumCartridgeItem) item("cbcparts:medium_shrapnel_shell_cartridge")).filled());

        BlockEntity mountBe = helper.getBlockEntity(mountRel);
        invoke(mountBe, "assemble");
        PitchOrientedContraptionEntity cannon = (PitchOrientedContraptionEntity) invoke(mountBe, "getContraption");
        helper.assertTrue(cannon != null, "cannon did not assemble");
        MountedMediumcannonContraption contraption = (MountedMediumcannonContraption) cannon.getContraption();
        contraption.fireShot(level, cannon);

        List<? extends Entity> shots = level.getEntities(type("cbcparts:medium_shrapnel_shell"), cannon.getBoundingBox().inflate(16), e -> true);
        helper.assertTrue(shots.size() == 1, "rounds fired: " + shots.size());
        Vec3 velocity = shots.getFirst().getDeltaMovement();
        helper.assertTrue(velocity.z > 1, "round flies out of the muzzle (south): " + velocity);
        shots.forEach(Entity::discard);
        ItemStack left = ((MediumcannonBreechBlockEntity) contraption.presentBlockEntities.get(contraption.getStartPos())).getInputBuffer();
        helper.assertTrue(left.is(item(NW + ":empty_mediumcannon_cartridge")), "breech holds the spent case: " + left);
        CBCPartsAssembly.LOGGER.info("[test] medium cannon fired a shrapnel round at {} blocks/tick", velocity.length());
        cannon.discard();
        helper.succeed();
    }

    private static Object invoke(Object target, String name) throws Exception {
        for (Class<?> c = target.getClass(); c != null; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                if (m.getName().equals(name) && m.getParameterCount() == 0) {
                    m.setAccessible(true);
                    return m.invoke(target);
                }
            }
        }
        throw new NoSuchMethodException(target.getClass().getName() + "." + name);
    }

    /** Every round flies like Neo Warfare's own medium rounds: same gravity and drag. */
    public static void fliesLikeNeoWarfareRounds(GameTestHelper helper) throws Exception {
        var nw = riftyboi.cbcmodernwarfare.index.CBCModernWarfareMunitionPropertiesHandlers.HE_MEDIUMCANNON
                .getPropertiesOf(type(NW + ":he_mediumshell")).ballistics();
        for (MediumShells.Entry e : MediumShells.entries()) {
            Object properties = e.shell().kind().handler().getPropertiesOf(e.entity().get());
            Method accessor = properties.getClass().getMethod("ballistics");
            accessor.setAccessible(true);
            var b = (rbasamoyai.createbigcannons.munitions.config.components.BallisticPropertiesComponent) accessor.invoke(properties);
            helper.assertTrue(b.gravity() == nw.gravity() && b.drag() == nw.drag() && b.isQuadraticDrag() == nw.isQuadraticDrag(),
                    e.shell().name() + ": gravity " + b.gravity() + ", drag " + b.drag() + "; Neo Warfare HE round " + nw.gravity() + ", " + nw.drag());
        }
        helper.succeed();
    }
}
