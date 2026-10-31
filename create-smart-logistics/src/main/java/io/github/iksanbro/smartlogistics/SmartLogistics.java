package io.github.iksanbro.smartlogistics;

import com.mojang.logging.LogUtils;
import io.github.iksanbro.smartlogistics.client.ClientSetup;
import io.github.iksanbro.smartlogistics.network.Payloads;
import io.github.iksanbro.smartlogistics.registry.ModBlockEntities;
import io.github.iksanbro.smartlogistics.registry.ModBlocks;
import io.github.iksanbro.smartlogistics.registry.ModItems;
import io.github.iksanbro.smartlogistics.registry.ModMenus;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

@Mod(SmartLogistics.MOD_ID)
public final class SmartLogistics {
    public static final String MOD_ID = "smartlogistics";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SmartLogistics(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, Config.SPEC);
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModItems.TABS.register(modBus);
        ModBlockEntities.TYPES.register(modBus);
        ModMenus.MENUS.register(modBus);
        modBus.addListener(ModBlockEntities::registerCapabilities);
        modBus.addListener(Payloads::register);
        if (FMLEnvironment.dist.isClient()) ClientSetup.init(modBus);
    }
}
