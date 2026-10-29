package io.github.iksanbro.cbcparts;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    private Config() {}

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue CONNECT_IN_SURVIVAL = BUILDER
            .comment("Connect cannon parts placed in survival to neighbouring parts, the same way",
                    "Create Big Cannons does it in creative mode. Applies to autocannons (CBC),",
                    "twin/heavy autocannons and rocket pods (CBC: Advanced Technologies),",
                    "medium and rotary cannons (CBC Neo Warfare). Big cannons are not affected.")
            .define("connectPartsInSurvival", true);

    public static final ModConfigSpec.BooleanValue CONNECT_FOR_MACHINES = BUILDER
            .comment("Also connect parts placed by machines and fake players (for example Create deployers).")
            .define("connectPartsPlacedByMachines", true);

    public static final ModConfigSpec.BooleanValue WRENCH_PICKUP = BUILDER
            .comment("Sneak + right click with a wrench takes a cannon part (every part this mod has a",
                    "recipe for) back into the inventory as the part itself.")
            .define("wrenchPickupParts", true);

    public static final ModConfigSpec SPEC = BUILDER.build();
}
