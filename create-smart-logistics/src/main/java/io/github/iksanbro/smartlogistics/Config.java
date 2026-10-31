package io.github.iksanbro.smartlogistics;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    private Config() {}

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static {
        BUILDER.push("fixes");
    }

    public static final ModConfigSpec.BooleanValue CHAIN_JAM_FIX = BUILDER
            .comment("Chain conveyors that are full no longer refuse packages from neighbouring chain conveyors.",
                    "Without this, two full conveyors that want to pass packages to each other wait forever",
                    "and the whole network stops. The capacity still limits packages put on by frogports.")
            .define("chainConveyorJamFix", true);

    public static final ModConfigSpec.DoubleValue CHAIN_OVERFLOW = BUILDER
            .comment("How many times its capacity a chain conveyor may hold when packages arrive from other",
                    "chain conveyors. A package that swaps places with one going the other way always passes.")
            .defineInRange("chainConveyorOverflowFactor", 3.0, 1.0, 20.0);

    public static final ModConfigSpec.BooleanValue GAUGES_IGNORE_UNLOADED = BUILDER
            .comment("Factory gauges keep working when a part of their logistics network is in an unloaded chunk,",
                    "counting only the stock that is loaded, instead of showing '?' and waiting.")
            .define("gaugesIgnoreUnloadedLinks", true);

    static {
        BUILDER.pop().push("smart_blocks");
    }

    public static final ModConfigSpec.IntValue FROGPORT_BATCH = BUILDER
            .comment("Packages a smart frogport sends or catches at a time.")
            .defineInRange("smartFrogportBatch", 5, 1, 32);

    public static final ModConfigSpec.IntValue PACKAGER_BATCH = BUILDER
            .comment("Boxes a smart packager packs or unpacks at a time.")
            .defineInRange("smartPackagerBatch", 5, 1, 32);

    static {
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();
}
