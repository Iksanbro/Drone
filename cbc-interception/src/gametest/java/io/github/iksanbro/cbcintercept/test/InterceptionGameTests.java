package io.github.iksanbro.cbcintercept.test;

import io.github.iksanbro.cbcintercept.CBCInterception;
import io.github.iksanbro.cbcintercept.Config;
import io.github.iksanbro.cbcintercept.Detonation;
import io.github.iksanbro.cbcintercept.Interception;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import rbasamoyai.createbigcannons.index.CBCDataComponents;
import rbasamoyai.createbigcannons.munitions.AbstractCannonProjectile;
import rbasamoyai.createbigcannons.munitions.autocannon.AbstractAutocannonProjectile;
import rbasamoyai.createbigcannons.munitions.autocannon.flak.FlakAutocannonProjectile;

/**
 * Projectiles are spawned high in the air and sent at each other. All tests run at the same time in
 * one batch, each at its own height so their projectiles never meet. The test structure ("flight",
 * 48 x 16 blocks of air) covers every flight path, so the game keeps those chunks ticking. Tests
 * that change the config start after the rest.
 */
@GameTestHolder(CBCInterception.MOD_ID)
@PrefixGameTestTemplate(false)
public final class InterceptionGameTests {
    private static final List<Vec3> EXPLOSIONS = new CopyOnWriteArrayList<>();

    static {
        NeoForge.EVENT_BUS.addListener((ExplosionEvent.Detonate e) -> EXPLOSIONS.add(e.getExplosion().center()));
    }

    /** Launches right away and runs the check that {@code launch} returned {@code ticks} later. */
    private static void flight(GameTestHelper helper, int ticks, java.util.function.Supplier<Runnable> launch) {
        Runnable check = launch.get();
        helper.runAfterDelay(ticks, () -> {
            check.run();
            helper.succeed();
        });
    }

    private static Entity spawn(GameTestHelper helper, String id, double height, Vec3 relative, Vec3 motion) {
        ServerLevel level = helper.getLevel();
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(id));
        Entity e = type.create(level);
        if (e == null) throw new IllegalStateException("cannot create " + id);
        Vec3 at = helper.absoluteVec(relative.add(0, height, 0));
        e.moveTo(at.x, at.y, at.z, 0, 0);
        e.setDeltaMovement(motion);
        e.setNoGravity(true); // straight flight: the test aims exactly
        if (e instanceof AbstractAutocannonProjectile round) round.setLifetime(200);
        if (e instanceof AbstractCannonProjectile p) p.setOrientation(motion.normalize());
        if (!level.addFreshEntity(e)) throw new IllegalStateException("cannot add " + id);
        return e;
    }

    private static String describe(Entity e) {
        return String.format("%s removed=%s at %.1f/%.1f/%.1f age=%d", BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath(),
                e.isRemoved(), e.getX(), e.getY(), e.getZ(), e.tickCount);
    }

    private static void cleanUp(Entity... entities) {
        for (Entity e : entities) if (!e.isRemoved()) e.discard();
    }

    private static boolean exploded(Vec3 near, double within) {
        return EXPLOSIONS.stream().anyMatch(p -> p.distanceTo(near) <= within);
    }

    /** Two autocannon AP rounds fired at each other: both break apart where they meet. */
    @GameTest(template = "flight", timeoutTicks = 100)
    public static void headOnRoundsHitEachOther(GameTestHelper helper) {
        double y = 40;
        flight(helper, 12, () -> {
            Entity a = spawn(helper, "createbigcannons:ap_autocannon", y, new Vec3(0, 0, 0.5), new Vec3(3, 0, 0));
            Entity b = spawn(helper, "createbigcannons:ap_autocannon", y, new Vec3(30, 0, 0.5), new Vec3(-3, 0, 0));
            double meet = helper.absoluteVec(new Vec3(15, 0, 0)).x;
            return () -> {
                String state = describe(a) + ", " + describe(b) + ", meeting point x=" + meet;
                boolean stopped = a.isRemoved() && b.isRemoved();
                cleanUp(a, b);
                helper.assertTrue(stopped, "rounds were not stopped: " + state);
                helper.assertTrue(a.getX() < meet + 3 && b.getX() > meet - 3, "rounds passed each other: " + state);
            };
        });
    }

    /** Rounds of one volley (twin barrels) fly side by side and must not hit each other. */
    @GameTest(template = "flight", timeoutTicks = 100)
    public static void roundsOfOneVolleyDoNotCollide(GameTestHelper helper) {
        double y = 60;
        flight(helper, 8, () -> {
            Entity a = spawn(helper, "createbigcannons:ap_autocannon", y, new Vec3(0, 0, 0.5), new Vec3(3, 0, 0));
            Entity b = spawn(helper, "createbigcannons:ap_autocannon", y, new Vec3(0, 0, 0.8), new Vec3(3, 0, 0));
            return () -> {
                String state = describe(a) + ", " + describe(b);
                boolean alive = !a.isRemoved() && !b.isRemoved();
                cleanUp(a, b);
                helper.assertTrue(a.tickCount > 4 && b.tickCount > 4, "rounds did not fly: " + state);
                helper.assertTrue(alive, "rounds flying side by side hit each other: " + state);
            };
        });
    }

    /** An HE shell hit by an AP round detonates in the air where it was hit. */
    @GameTest(template = "flight", timeoutTicks = 100)
    public static void hitHeShellDetonates(GameTestHelper helper) {
        double y = 80;
        flight(helper, 12, () -> {
            Entity shell = spawn(helper, "createbigcannons:he_shell", y, new Vec3(30, 0, 0.5), new Vec3(-2, 0, 0));
            Entity round = spawn(helper, "createbigcannons:ap_autocannon", y, new Vec3(0, 0, 0.5), new Vec3(4, 0, 0));
            return () -> {
                boolean boom = exploded(helper.absoluteVec(new Vec3(20, y, 0.5)), 6);
                String state = describe(shell) + ", " + describe(round) + ", explosions=" + EXPLOSIONS;
                boolean hit = shell.isRemoved() && round.isRemoved();
                cleanUp(shell, round);
                helper.assertTrue(hit && boom, "HE shell did not detonate: " + state);
            };
        });
    }

    /** A flak round with an armed proximity fuze goes off next to an incoming shot and destroys it. */
    @GameTest(template = "flight", timeoutTicks = 100)
    public static void proximityFlakDestroysShot(GameTestHelper helper) {
        double y = 100;
        flight(helper, 14, () -> {
            // 31 blocks apart, closing at 5 a tick: the gap goes 6 -> 1, the fuze (4 blocks) acts in that
            // tick, before any direct hit (0.7 blocks) could happen in the next one
            Entity shot = spawn(helper, "createbigcannons:shot", y, new Vec3(31, 0, 1), new Vec3(-2, 0, 0));
            FlakAutocannonProjectile flak = (FlakAutocannonProjectile) spawn(helper, "createbigcannons:flak_autocannon", y, new Vec3(0, 0, 0.5), new Vec3(3, 0, 0));
            flak.setFuze(armedProximityFuze(4));
            return () -> {
                String state = describe(flak) + ", " + describe(shot);
                boolean flakOff = flak.isRemoved(), shotDown = shot.isRemoved();
                cleanUp(shot, flak);
                helper.assertTrue(flakOff, "proximity fuze did not go off: " + state);
                helper.assertTrue(shotDown, "flak did not destroy the shot: " + state);
            };
        });
    }

    /** A shell caught in an explosion goes off. */
    @GameTest(template = "flight", timeoutTicks = 100)
    public static void explosionSetsOffShell(GameTestHelper helper) {
        double y = 120;
        Entity[] shot = new Entity[1];
        int[] tick = {0};
        flight(helper, 6, () -> {
            shot[0] = spawn(helper, "createbigcannons:shot", y, new Vec3(2, 0, 2), new Vec3(0.3, 0, 0));
            return () -> {
                String state = describe(shot[0]);
                boolean removed = shot[0].isRemoved();
                cleanUp(shot[0]);
                helper.assertTrue(removed, "shot survived an explosion next to it: " + state);
            };
        });
        // 4 ticks after the launch an explosion goes off next to the shot
        helper.onEachTick(() -> {
            if (shot[0] != null && ++tick[0] == 4)
                helper.getLevel().explode(null, shot[0].getX() + 1, shot[0].getY(), shot[0].getZ(), 3, Level.ExplosionInteraction.NONE);
        });
    }

    /** HE autocannon round of CBC Neo Warfare shot down by a machine gun bullet. */
    @GameTest(template = "flight", timeoutTicks = 100)
    public static void neoWarfareRoundShotDown(GameTestHelper helper) {
        if (!ModList.get().isLoaded("cbcmodernwarfare")) {
            helper.succeed();
            return;
        }
        double y = 140;
        flight(helper, 12, () -> {
            Entity shell = spawn(helper, "cbcmodernwarfare:he_autocannon", y, new Vec3(30, 0, 0.5), new Vec3(-3, 0, 0));
            Entity bullet = spawn(helper, "createbigcannons:machine_gun_bullet", y, new Vec3(0, 0, 0.5), new Vec3(4, 0, 0));
            return () -> {
                boolean boom = exploded(helper.absoluteVec(new Vec3(17, y, 0.5)), 8);
                String state = describe(shell) + ", " + describe(bullet) + ", explosions=" + EXPLOSIONS;
                boolean hit = shell.isRemoved() && bullet.isRemoved();
                cleanUp(shell, bullet);
                helper.assertTrue(hit && boom, "HE round was not shot down: " + state);
            };
        });
    }

    /** A rocket of CBC Enchanced Shells hit by an AP round releases and sets off its warhead. */
    @GameTest(template = "flight", timeoutTicks = 100)
    public static void enhancedShellsRocketShotDown(GameTestHelper helper) throws Exception {
        if (!ModList.get().isLoaded("cbc_enhanced_shells")) {
            helper.succeed();
            return;
        }
        double y = 160;
        ServerLevel level = helper.getLevel();
        java.util.function.Function<String, net.minecraft.world.level.block.state.BlockState> block = id ->
                BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("cbc_enhanced_shells", id)).defaultBlockState();
        List<net.minecraft.world.level.block.state.BlockState> stack =
                List.of(block.apply("rocket_breech"), block.apply("rocket_fuselage"), block.apply("nose_cone_steel"));
        java.lang.reflect.Method create = Class.forName("com.example.cbcenhancedshells.rocket.RocketEntity").getMethod("create",
                ServerLevel.class, net.minecraft.core.BlockPos.class, net.minecraft.core.Direction.class, List.class,
                int.class, int.class, ItemStack.class, ItemStack.class, boolean.class);
        flight(helper, 14, () -> {
            Entity rocket;
            try {
                rocket = (Entity) create.invoke(null, level, net.minecraft.core.BlockPos.containing(helper.absoluteVec(new Vec3(40, y, 1))),
                        net.minecraft.core.Direction.WEST, stack, 200, 0, ItemStack.EMPTY, ItemStack.EMPTY, false);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
            rocket.setNoGravity(true);
            level.addFreshEntity(rocket);
            Entity round = spawn(helper, "createbigcannons:ap_autocannon", y, new Vec3(0, 0.5, 1.5), new Vec3(4, 0, 0));
            return () -> {
                String state = describe(rocket) + ", " + describe(round);
                boolean hit = rocket.isRemoved() && round.isRemoved();
                cleanUp(rocket, round);
                helper.assertTrue(hit, "rocket was not shot down: " + state);
            };
        });
    }

    /** Switched off in the config: rounds fly through each other. Starts after the tests above. */
    @GameTest(template = "flight", setupTicks = 120, timeoutTicks = 100)
    public static void disabledRoundsPassThrough(GameTestHelper helper) {
        double y = 40;
        boolean old = Config.ENABLED.get();
        Config.ENABLED.set(false);
        flight(helper, 12, () -> {
            Entity a = spawn(helper, "createbigcannons:ap_autocannon", y, new Vec3(0, 0, 0.5), new Vec3(3, 0, 0));
            Entity b = spawn(helper, "createbigcannons:ap_autocannon", y, new Vec3(30, 0, 0.5), new Vec3(-3, 0, 0));
            return () -> {
                Config.ENABLED.set(old);
                String state = describe(a) + ", " + describe(b);
                boolean alive = !a.isRemoved() && !b.isRemoved();
                cleanUp(a, b);
                helper.assertTrue(a.tickCount > 8 && alive, "rounds collided with the mod switched off: " + state);
            };
        });
    }

    /** With explosions switched off only the flak fragments can destroy the shot. Starts after the tests above. */
    @GameTest(template = "flight", setupTicks = 240, timeoutTicks = 100)
    public static void flakFragmentsDestroyShot(GameTestHelper helper) {
        double y = 60;
        boolean old = Config.EXPLOSIONS.get();
        Config.EXPLOSIONS.set(false);
        flight(helper, 16, () -> {
            // the fuze (5 blocks) goes off when the gap closes to 5, the flak is gone before a direct hit
            Entity shot = spawn(helper, "createbigcannons:shot", y, new Vec3(30, 0, 1), new Vec3(-2, 0, 0));
            FlakAutocannonProjectile flak = (FlakAutocannonProjectile) spawn(helper, "createbigcannons:flak_autocannon", y, new Vec3(0, 0, 0.5), new Vec3(3, 0, 0));
            flak.setFuze(armedProximityFuze(5));
            return () -> {
                Config.EXPLOSIONS.set(old);
                String state = describe(flak) + ", " + describe(shot);
                boolean both = flak.isRemoved() && shot.isRemoved();
                cleanUp(shot, flak);
                helper.assertTrue(both, "fragments did not destroy the shot: " + state);
            };
        });
    }

    /**
     * Every cannon projectile in the game (CBC and loaded addons) can be set off without an error.
     * Each one is created high above the test, apart from the others, and set off right away.
     */
    @GameTest(template = "flight", timeoutTicks = 100)
    public static void everyProjectileCanBeSetOff(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Map<String, String> kinds = new TreeMap<>();
        List<String> problems = new ArrayList<>();
        int i = 0;
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            Entity e;
            try {
                e = type.create(level);
            } catch (RuntimeException ex) {
                continue;
            }
            if (e == null || !Interception.isProjectile(e)) continue;
            String id = BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
            Detonation.Kind kind = Detonation.kindOf(e);
            kinds.put(id, kind.name());
            // a nuclear warhead in a test world is no help
            if (id.contains("nuke") || id.contains("hbomb")) continue;
            // a grid above the test: 4 x 4 columns, 12 blocks up
            Vec3 at = helper.absoluteVec(new Vec3((i % 4) * 5, 180 + (i / 16) * 12, ((i / 4) % 4) * 5));
            i++;
            e.moveTo(at.x, at.y, at.z, 0, 0);
            try {
                Detonation.setOff(level, e, at);
            } catch (RuntimeException ex) {
                problems.add(id + ": " + ex);
            }
            if (!e.isRemoved()) problems.add(id + " was not removed");
        }
        CBCInterception.LOGGER.info("[test] cannon projectile types: {}, how each goes off: {}", kinds.size(), kinds);
        helper.assertTrue(problems.isEmpty(), "problems: " + problems);
        helper.assertTrue("DETONATE".equals(kinds.get("createbigcannons:he_shell")), "HE shell must detonate");
        helper.assertTrue("BREAK".equals(kinds.get("createbigcannons:ap_autocannon")), "AP round must break apart");
        if (ModList.get().isLoaded("cbc_enhanced_shells")) {
            helper.assertTrue("ROCKET_WARHEAD".equals(kinds.get("cbc_enhanced_shells:rocket")), "rocket must release its warhead");
            if (kinds.containsKey("cbc_enhanced_shells:small_missile"))
                helper.assertTrue("MISSILE".equals(kinds.get("cbc_enhanced_shells:small_missile")), "small missile must detonate");
        }
        helper.runAfterDelay(40, helper::succeed); // let the explosions and fragments finish
    }

    private static ItemStack armedProximityFuze(int distance) {
        ItemStack fuze = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("createbigcannons:proximity_fuze")));
        fuze.set(CBCDataComponents.ARMED, true);
        fuze.set(CBCDataComponents.DETONATION_DISTANCE, distance);
        return fuze;
    }
}
