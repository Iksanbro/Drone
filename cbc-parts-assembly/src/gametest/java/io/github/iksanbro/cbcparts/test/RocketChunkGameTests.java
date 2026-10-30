package io.github.iksanbro.cbcparts.test;

import io.github.iksanbro.cbcparts.CBCPartsAssembly;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Rockets of CBC Enchanced Shells must keep flying through chunks nobody has loaded. */
@GameTestHolder(CBCPartsAssembly.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RocketChunkGameTests {
    private static final String ES = "cbc_enhanced_shells";

    private static BlockState block(String id) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(ES, id)).defaultBlockState();
    }

    /** Control: a plain CBC HE shell at rocket speed, kept flying by CBC and RPL themselves. */
    @GameTest(template = "empty", timeoutTicks = 420)
    public static void controlCbcShellFlight(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos start = helper.absolutePos(new BlockPos(5, 60, 5));
        Entity shell = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("createbigcannons:he_shell")).create(level);
        shell.setPos(start.getX() + 0.5, start.getY(), start.getZ() + 0.5);
        shell.setNoGravity(true);
        shell.setDeltaMovement(-5, 0, 0); // west, away from the rocket tests
        level.addFreshEntity(shell);
        int[] stalled = {0};
        double[] last = {shell.getX()};
        helper.onEachTick(() -> {
            if (shell.isRemoved()) return;
            if (shell.getX() == last[0]) stalled[0]++;
            last[0] = shell.getX();
        });
        helper.runAfterDelay(400, () -> {
            CBCPartsAssembly.LOGGER.info("[test] control CBC shell after 400 ticks: removed={} ({}), distance={}, ticks without moving={}",
                    shell.isRemoved(), shell.getRemovalReason(), (int) (start.getX() - shell.getX()), stalled[0]);
            shell.discard();
            helper.succeed();
        });
    }

    /** The usual case: the terrain on the path was generated before but is not loaded now. */
    @GameTest(template = "empty", timeoutTicks = 420)
    public static void rocketFliesThroughUnloadedChunks(GameTestHelper helper) throws Exception {
        launch(helper, true, Direction.EAST);
    }

    /** Terrain nobody has visited: chunks are generated as the rocket comes. */
    @GameTest(template = "empty", timeoutTicks = 420)
    public static void rocketFliesOverNewTerrain(GameTestHelper helper) throws Exception {
        launch(helper, false, Direction.NORTH);
    }

    private static void launch(GameTestHelper helper, boolean pregenerate, Direction dir) throws Exception {
        if (!ModList.get().isLoaded(ES)) {
            helper.succeed();
            return;
        }
        ServerLevel level = helper.getLevel();
        BlockPos start = helper.absolutePos(new BlockPos(5, 60, 5));
        if (pregenerate) {
            // generate a band along the path once, as when players have been there before;
            // nothing keeps these chunks loaded afterwards
            for (int along = 0; along <= 1600; along += 16)
                for (int side = -48; side <= 48; side += 16)
                    level.getChunk(start.relative(dir, along).relative(dir.getClockWise(), side));
        }
        List<BlockState> stack = List.of(block("rocket_breech"), block("rocket_fuselage"), block("rocket_fuselage"), block("nose_cone_steel"));
        Class<?> rocketClass = Class.forName("com.example.cbcenhancedshells.rocket.RocketEntity");
        Method create = rocketClass.getMethod("create", ServerLevel.class, BlockPos.class, Direction.class, List.class,
                int.class, int.class, ItemStack.class, ItemStack.class, boolean.class);
        Entity rocket = (Entity) create.invoke(null, level, start, dir, stack, 200, 0, ItemStack.EMPTY, ItemStack.EMPTY, false);
        rocket.setNoGravity(true);
        level.addFreshEntity(rocket);

        List<String> track = new ArrayList<>();
        int[] stalled = {0};
        java.util.function.ToDoubleFunction<Entity> along = e -> e.position().subtract(start.getCenter()).dot(net.minecraft.world.phys.Vec3.atLowerCornerOf(dir.getNormal()));
        double[] last = {along.applyAsDouble(rocket)};
        helper.onEachTick(() -> {
            if (rocket.isRemoved()) return;
            boolean ticking = level.isPositionEntityTicking(rocket.blockPosition());
            double now = along.applyAsDouble(rocket);
            if (now == last[0]) stalled[0]++;
            last[0] = now;
            if (level.getGameTime() % 20 == 0)
                track.add(String.format("d=%.0f ticking=%s v=%.1f", now, ticking, rocket.getDeltaMovement().length()));
        });
        helper.runAfterDelay(400, () -> {
            double distance = along.applyAsDouble(rocket);
            rocket.discard(); // stop it, or it keeps loading chunks while the server shuts down
            CBCPartsAssembly.LOGGER.info("[test] rocket ({}) after 400 ticks: removed={} ({}), distance={} blocks, ticks without moving={}, track={}",
                    pregenerate ? "generated terrain" : "new terrain", rocket.isRemoved(), rocket.getRemovalReason(), (int) distance, stalled[0], track);
            // with no help it stops within ~100 blocks of the test area (the control shell shows that)
            helper.assertTrue(distance > 1500, "rocket stopped after " + (int) distance + " blocks");
            helper.assertTrue(stalled[0] < 20, "rocket stood still for " + stalled[0] + " of 400 ticks");
            helper.succeed();
        });
    }
}
