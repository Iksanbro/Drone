package io.github.iksanbro.wpfix;

import net.neoforged.fml.common.Mod;

/** The fix is a mixin (see {@code mixin/TornadoEventMixin}); the mod class only registers the mod. */
@Mod(WeatherPhysicsSableFix.MOD_ID)
public final class WeatherPhysicsSableFix {
    public static final String MOD_ID = "wpfix";
}
