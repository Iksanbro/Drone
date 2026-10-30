package io.github.iksanbro.cbcintercept;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    private Config() {}

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLED = BUILDER
            .comment("Cannon projectiles that meet in flight hit each other: shells, autocannon and machine gun",
                    "rounds, rockets and missiles of Create Big Cannons and its addons.")
            .define("projectilesCollide", true);

    public static final ModConfigSpec.DoubleValue EXTRA_HIT_RADIUS = BUILDER
            .comment("Blocks added to the size of the two projectiles when checking whether they met.",
                    "0 = only touching projectiles collide; larger values make shooting down easier.")
            .defineInRange("extraHitRadius", 0.5, 0.0, 8.0);

    public static final ModConfigSpec.BooleanValue FRAGMENTS = BUILDER
            .comment("Fragments of flak, shrapnel, grapeshot and similar bursts hit projectiles too.")
            .define("fragmentsHitProjectiles", true);

    public static final ModConfigSpec.BooleanValue EXPLOSIONS = BUILDER
            .comment("Projectiles inside the radius of an explosion are set off by it.",
                    "A projectile flying along with the one that exploded (same volley) is not affected.")
            .define("explosionsHitProjectiles", true);

    public static final ModConfigSpec.BooleanValue PROXIMITY_FUZES = BUILDER
            .comment("An armed proximity fuze also goes off when an enemy projectile passes within its",
                    "detonation distance (Create Big Cannons only reacts to living things and vehicles).")
            .define("proximityFuzesReactToProjectiles", true);

    public static final ModConfigSpec SPEC = BUILDER.build();
}
