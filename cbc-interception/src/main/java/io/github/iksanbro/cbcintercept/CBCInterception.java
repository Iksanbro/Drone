package io.github.iksanbro.cbcintercept;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(CBCInterception.MOD_ID)
public final class CBCInterception {
    public static final String MOD_ID = "cbcintercept";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CBCInterception(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, Config.SPEC);
        NeoForge.EVENT_BUS.addListener(Interception::onEntityJoin);
        NeoForge.EVENT_BUS.addListener(Interception::onLevelTick);
        NeoForge.EVENT_BUS.addListener(Interception::onExplosion);
    }
}
