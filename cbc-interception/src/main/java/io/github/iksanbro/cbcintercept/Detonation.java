package io.github.iksanbro.cbcintercept;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Position;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import rbasamoyai.createbigcannons.munitions.big_cannon.AbstractBigCannonProjectile;

/**
 * Sets off a projectile that was hit in flight, the way the projectile itself goes off.
 *
 * <p>The mods have no shared "explode now" method, but they follow a few patterns, found once per
 * class:
 * <ul>
 * <li>{@code detonate(Position)}: every explosive projectile of Create Big Cannons (HE, shrapnel,
 * AP, smoke, fluid, mortar shells, flak), CBC: Advanced Technologies (HE/HEI rounds, heavy
 * autocannon shells, fuzed rockets, cluster shells), CBC Neo Warfare (medium cannon and
 * autocannon shells, HEAP/HEF shells, smoke discharger) and CBC Enchanced Shells shells;</li>
 * <li>{@code detonate(ServerLevel, Vec3, boolean)}: small missiles of CBC Enchanced Shells;</li>
 * <li>{@code releaseProjectile(Vec3, Vec3, boolean, boolean)}: rockets of CBC Enchanced Shells, which
 * release their warhead shell and set it off;</li>
 * <li>anything else (solid shot, AP rounds, bullets, unfuzed rockets) breaks apart.</li>
 * </ul>
 */
public final class Detonation {
    private Detonation() {}

    public enum Kind { DETONATE, MISSILE, ROCKET_WARHEAD, BREAK }

    private record Strategy(Kind kind, Method method) {}

    private static final Map<Class<?>, Strategy> STRATEGIES = new ConcurrentHashMap<>();
    private static final Set<Class<?>> FAILED = ConcurrentHashMap.newKeySet();

    public static Kind kindOf(Entity projectile) {
        return strategy(projectile.getClass()).kind();
    }

    private static Strategy strategy(Class<?> type) {
        return STRATEGIES.computeIfAbsent(type, Detonation::find);
    }

    private static Strategy find(Class<?> type) {
        Method m = method(type, "detonate", Position.class);
        if (m != null) return new Strategy(Kind.DETONATE, m);
        m = method(type, "detonate", ServerLevel.class, Vec3.class, boolean.class);
        if (m != null) return new Strategy(Kind.MISSILE, m);
        m = method(type, "releaseProjectile", Vec3.class, Vec3.class, boolean.class, boolean.class);
        if (m != null) return new Strategy(Kind.ROCKET_WARHEAD, m);
        return new Strategy(Kind.BREAK, null);
    }

    /**
     * The implementation the class has (its own or inherited), or null. Searched from the class up, so
     * the first declaration found is the most specific one; an abstract one means there is no
     * implementation (CBC Neo Warfare adds an abstract {@code detonate(Position)} to every CBC projectile).
     */
    private static Method method(Class<?> type, String name, Class<?>... params) {
        for (Class<?> c = type; c != null && c != Entity.class; c = c.getSuperclass()) {
            try {
                Method m = c.getDeclaredMethod(name, params);
                if (Modifier.isAbstract(m.getModifiers())) return null;
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException ignored) {
                // look in the superclass
            } catch (RuntimeException e) {
                CBCInterception.LOGGER.warn("Cannot use {}.{}: {}", c.getName(), name, e.toString());
                return null;
            }
        }
        return null;
    }

    /** Sets the projectile off at {@code at} and removes it. */
    public static void setOff(ServerLevel level, Entity projectile, Vec3 at) {
        if (projectile.isRemoved()) return;
        projectile.setPos(at);
        Strategy strategy = strategy(projectile.getClass());
        try {
            switch (strategy.kind()) {
                case DETONATE -> strategy.method().invoke(projectile, at);
                case MISSILE -> strategy.method().invoke(projectile, level, at, false);
                case ROCKET_WARHEAD -> strategy.method().invoke(projectile, at, projectile.getDeltaMovement(), true, true);
                case BREAK -> breakApart(level, projectile, at);
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            if (FAILED.add(projectile.getClass()))
                CBCInterception.LOGGER.error("Could not set off {}, it breaks apart instead", projectile.getType(), e);
            breakApart(level, projectile, at);
        }
        if (!projectile.isRemoved()) projectile.discard();
    }

    private static void breakApart(ServerLevel level, Entity projectile, Vec3 at) {
        ParticleOptions particle = projectile instanceof AbstractBigCannonProjectile shell && !shell.getRenderedBlockState().isAir()
                ? new BlockParticleOption(ParticleTypes.BLOCK, shell.getRenderedBlockState())
                : ParticleTypes.CRIT;
        level.sendParticles(particle, at.x, at.y, at.z, 12, 0.2, 0.2, 0.2, 0.1);
        level.sendParticles(ParticleTypes.SMOKE, at.x, at.y, at.z, 6, 0.1, 0.1, 0.1, 0.02);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.ANVIL_LAND, SoundSource.NEUTRAL, 1.5f, 1.6f);
        projectile.discard();
    }
}
