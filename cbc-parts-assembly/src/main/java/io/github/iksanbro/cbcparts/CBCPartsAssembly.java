package io.github.iksanbro.cbcparts;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(CBCPartsAssembly.MOD_ID)
public final class CBCPartsAssembly {
    public static final String MOD_ID = "cbcparts";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CBCPartsAssembly(IEventBus modBus, ModContainer container) {
        ModItems.register(modBus);
        container.registerConfig(ModConfig.Type.SERVER, Config.SPEC);
        // Lowest priority: run only when no other mod cancelled the placement.
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, CannonPartConnector::onBlockPlaced);
    }
}
