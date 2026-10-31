package io.github.iksanbro.smartlogistics.client;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import io.github.iksanbro.smartlogistics.SmartLogistics;
import net.minecraft.resources.ResourceLocation;

/** Models of the smart frogport's moving parts: Create's frog with the smart textures. */
public final class SmartPartials {
    private SmartPartials() {}

    public static final PartialModel FROGPORT_BODY = block("smart_frogport/body");
    public static final PartialModel FROGPORT_HEAD = block("smart_frogport/head");
    public static final PartialModel FROGPORT_HEAD_GOGGLES = block("smart_frogport/head_goggles");
    public static final PartialModel FROGPORT_TONGUE = block("smart_frogport/tongue");

    private static PartialModel block(String path) {
        return PartialModel.of(ResourceLocation.fromNamespaceAndPath(SmartLogistics.MOD_ID, "block/" + path));
    }

    /** Loads the class so the models are registered before models bake. */
    public static void init() {}
}
