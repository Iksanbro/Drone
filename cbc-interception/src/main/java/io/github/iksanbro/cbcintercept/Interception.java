package io.github.iksanbro.cbcintercept;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import rbasamoyai.createbigcannons.index.CBCDataComponents;
import rbasamoyai.createbigcannons.munitions.AbstractCannonProjectile;
import rbasamoyai.createbigcannons.munitions.big_cannon.fluid_shell.FluidBlobBurst;
import rbasamoyai.createbigcannons.munitions.fuzes.ProximityFuzeItem;
import rbasamoyai.createbigcannons.remix.CustomExplosion;
import rbasamoyai.ritchiesprojectilelib.projectile_burst.ProjectileBurst;

/**
 * Finds cannon projectiles that met in flight.
 *
 * <p>Create Big Cannons checks what a projectile hits against where other entities stand, and it
 * leaves other projectiles out on purpose. Two projectiles both move, often several blocks per
 * tick, so this mod checks them itself once all entities moved: for every pair it follows their
 * straight paths during the tick and finds the moment they came closer than their sizes (plus
 * {@link Config#EXTRA_HIT_RADIUS}); they hit each other there. Both go off, see {@link Detonation}.
 */
public final class Interception {
    private Interception() {}

    /** Entity types that take part although they are not Create Big Cannons projectiles (rockets of other mods). */
    public static final TagKey<EntityType<?>> INTERCEPTABLE =
            TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(CBCInterception.MOD_ID, "interceptable"));
    /** Projectiles and bursts that take no part. */
    public static final TagKey<EntityType<?>> NOT_INTERCEPTABLE =
            TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(CBCInterception.MOD_ID, "not_interceptable"));

    /** A projectile that flew fewer ticks is still at the muzzle (and its last position is not known yet). */
    static final int ARMING_TICKS = 2;
    /** Projectiles moving slower than this relative to each other fly together: one volley, twin barrels. */
    static final double SAME_VOLLEY_SPEED = 1.0;
    /** A longer move in one tick is a teleport, not flight. */
    private static final double MAX_STEP = 1024;

    private static final Set<Entity> TRACKED = Collections.newSetFromMap(new WeakHashMap<>());
    private static final Map<ServerLevel, List<Entity>> CAUGHT_IN_EXPLOSIONS = new WeakHashMap<>();
    private static final Map<Class<?>, Optional<Field>> FUZE_FIELDS = new ConcurrentHashMap<>();

    public static boolean isProjectile(Entity entity) {
        EntityType<?> type = entity.getType();
        return !type.is(NOT_INTERCEPTABLE) && (entity instanceof AbstractCannonProjectile || type.is(INTERCEPTABLE));
    }

    public static boolean isFragmentBurst(Entity entity) {
        return entity instanceof ProjectileBurst && !(entity instanceof FluidBlobBurst) && !entity.getType().is(NOT_INTERCEPTABLE);
    }

    public static void onEntityJoin(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (event.getLevel() instanceof ServerLevel && (isProjectile(entity) || isFragmentBurst(entity))) TRACKED.add(entity);
    }

    /** Straight path of a projectile during the last tick. */
    record Path(Entity entity, Vec3 from, Vec3 move, double radius, AABB box) {
        static Path of(Entity e) {
            Vec3 to = e.position();
            Vec3 from = new Vec3(e.xo, e.yo, e.zo);
            if (from.distanceToSqr(to) > MAX_STEP * MAX_STEP) from = to;
            double radius = Math.max(e.getBbWidth(), e.getBbHeight()) / 2;
            return new Path(e, from, to.subtract(from), radius, new AABB(from, to).inflate(radius));
        }

        Vec3 at(double t) {
            return from.add(move.scale(t));
        }

        boolean flying() {
            return entity.tickCount >= ARMING_TICKS && !entity.isRemoved()
                    && !(entity instanceof AbstractCannonProjectile p && p.isInGround());
        }
    }

    /**
     * Fraction of the tick (0..1) when two straight paths first came within {@code range} of each
     * other, or -1 if they did not. That is the moment of contact, or the moment a proximity fuze
     * notices the target, not the moment the two are closest.
     */
    static double contactTime(Vec3 fromA, Vec3 moveA, Vec3 fromB, Vec3 moveB, double range) {
        Vec3 apart = fromA.subtract(fromB);
        Vec3 relative = moveA.subtract(moveB);
        double range2 = range * range;
        double c = apart.lengthSqr() - range2;
        if (c <= 0) return 0; // already within range when the tick began
        double a = relative.lengthSqr();
        if (a < 1e-9) return -1;
        double b = apart.dot(relative);
        double disc = b * b - a * c;
        if (b >= 0 || disc < 0) return -1; // moving apart, or passing wide
        double t = (-b - Math.sqrt(disc)) / a;
        return t <= 1 ? t : -1;
    }

    static boolean sameVolley(Path a, Path b) {
        return a.move.subtract(b.move).lengthSqr() < SAME_VOLLEY_SPEED * SAME_VOLLEY_SPEED;
    }

    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        List<Entity> caught = CAUGHT_IN_EXPLOSIONS.remove(level);
        if (TRACKED.isEmpty() || !Config.ENABLED.get()) return;

        List<Path> shots = new ArrayList<>();
        List<ProjectileBurst> bursts = new ArrayList<>();
        TRACKED.removeIf(Entity::isRemoved);
        for (Entity e : TRACKED) {
            if (e.level() != level) continue;
            if (e instanceof ProjectileBurst burst) bursts.add(burst);
            else {
                Path path = Path.of(e);
                if (path.flying()) shots.add(path);
            }
        }

        Map<Entity, Vec3> hits = new LinkedHashMap<>();
        if (caught != null) {
            for (Entity e : caught) if (!e.isRemoved()) hits.putIfAbsent(e, e.position());
        }
        double extra = Config.EXTRA_HIT_RADIUS.get();

        // projectiles hitting each other
        for (int i = 0; i < shots.size(); i++) {
            Path a = shots.get(i);
            for (int j = i + 1; j < shots.size() && !hits.containsKey(a.entity); j++) {
                Path b = shots.get(j);
                if (hits.containsKey(b.entity) || !a.box.inflate(extra).intersects(b.box) || sameVolley(a, b)) continue;
                double t = contactTime(a.from, a.move, b.from, b.move, a.radius + b.radius + extra);
                if (t >= 0) {
                    hits.put(a.entity, a.at(t));
                    hits.put(b.entity, b.at(t));
                    CBCInterception.LOGGER.debug("{} and {} collided at {}", a.entity, b.entity, a.at(t));
                }
            }
        }

        // armed proximity fuzes going off next to a projectile
        if (Config.PROXIMITY_FUZES.get()) {
            for (Path a : shots) {
                if (hits.containsKey(a.entity)) continue;
                double distance = proximityDistance(a.entity);
                if (distance <= 0) continue;
                for (Path b : shots) {
                    if (b == a || !a.box.inflate(distance).intersects(b.box) || sameVolley(a, b)) continue;
                    double t = contactTime(a.from, a.move, b.from, b.move, distance + b.radius);
                    if (t >= 0) {
                        hits.put(a.entity, a.at(t));
                        break;
                    }
                }
            }
        }

        // fragments of flak, shrapnel and grapeshot
        if (Config.FRAGMENTS.get()) {
            for (ProjectileBurst burst : bursts) {
                if (burst.tickCount < 1 || burst.isRemoved()) continue;
                Vec3 base = burst.position();
                double radius = Math.max(burst.getSubProjectileWidth(), burst.getSubProjectileHeight()) / 2;
                for (ProjectileBurst.SubProjectile sub : burst.getSubProjectiles()) {
                    double[] d = sub.displacement(), v = sub.velocity();
                    Vec3 move = new Vec3(v[0], v[1], v[2]);
                    Vec3 to = base.add(d[0], d[1], d[2]);
                    Vec3 from = to.subtract(move);
                    AABB box = new AABB(from, to).inflate(radius + extra);
                    for (Path b : shots) {
                        if (hits.containsKey(b.entity) || !box.intersects(b.box)) continue;
                        double t = contactTime(from, move, b.from, b.move, radius + b.radius + extra);
                        if (t >= 0) hits.put(b.entity, b.at(t));
                    }
                }
            }
        }

        hits.forEach((projectile, at) -> Detonation.setOff(level, projectile, at));
    }

    /** Projectiles inside an explosion go off at the end of the tick (not in the middle of the explosion). */
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !Config.ENABLED.get() || !Config.EXPLOSIONS.get()) return;
        Explosion explosion = event.getExplosion();
        Vec3 center = explosion.center();
        double radius = explosion instanceof CustomExplosion custom ? custom.getEntityRadius() : explosion.radius();
        Entity source = explosion.getDirectSourceEntity();
        boolean fromProjectile = source != null && isProjectile(source);
        for (Entity e : event.getAffectedEntities()) {
            if (e == source || e.isRemoved() || !isProjectile(e) || !Path.of(e).flying()) continue;
            if (e.position().distanceTo(center) > radius + Math.max(e.getBbWidth(), e.getBbHeight()) / 2) continue;
            // the next round of the same burst of fire is not set off by the one in front of it
            if (fromProjectile && e.getDeltaMovement().subtract(source.getDeltaMovement()).length() < SAME_VOLLEY_SPEED) continue;
            CAUGHT_IN_EXPLOSIONS.computeIfAbsent(level, l -> new ArrayList<>()).add(e);
        }
    }

    /** Detonation distance of an armed proximity fuze in the projectile, or 0. */
    static double proximityDistance(Entity projectile) {
        Optional<Field> field = FUZE_FIELDS.computeIfAbsent(projectile.getClass(), Interception::findFuzeField);
        if (field.isEmpty()) return 0;
        try {
            ItemStack fuze = (ItemStack) field.get().get(projectile);
            if (fuze == null || !(fuze.getItem() instanceof ProximityFuzeItem) || !fuze.has(CBCDataComponents.ARMED)) return 0;
            return Math.max(1, fuze.getOrDefault(CBCDataComponents.DETONATION_DISTANCE, 1));
        } catch (ReflectiveOperationException | ClassCastException e) {
            return 0;
        }
    }

    /** The fuze of fuzed shells and rounds is a private field named {@code fuze} in CBC and the addons. */
    private static Optional<Field> findFuzeField(Class<?> type) {
        for (Class<?> c = type; c != null && c != Entity.class; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField("fuze");
                if (f.getType() != ItemStack.class) continue;
                f.setAccessible(true);
                return Optional.of(f);
            } catch (NoSuchFieldException ignored) {
                // look in the superclass
            } catch (RuntimeException e) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }
}
