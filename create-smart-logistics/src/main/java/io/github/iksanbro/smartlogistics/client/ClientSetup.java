package io.github.iksanbro.smartlogistics.client;

import com.simibubi.create.content.logistics.packager.PackagerRenderer;
import com.simibubi.create.content.logistics.packager.PackagerVisual;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import io.github.iksanbro.smartlogistics.registry.ModBlockEntities;
import io.github.iksanbro.smartlogistics.registry.ModBlocks;
import io.github.iksanbro.smartlogistics.registry.ModMenus;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public final class ClientSetup {
    private ClientSetup() {}

    public static void init(IEventBus modBus) {
        SmartPartials.init();
        modBus.addListener(ClientSetup::screens);
        modBus.addListener(ClientSetup::renderers);
        modBus.addListener(ClientSetup::setup);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(Tooltips::onTooltip);
    }

    private static void screens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.SMART_FROGPORT.get(), SmartFrogportScreen::new);
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.SMART_FROGPORT.get(), SmartFrogportRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.SMART_PACKAGER.get(), PackagerRenderer::new);
    }

    @SuppressWarnings("deprecation")
    private static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // the same frog and packager tray animations as Create's blocks
            SimpleBlockEntityVisualizer.builder(ModBlockEntities.SMART_FROGPORT.get())
                    .factory(SmartFrogportVisual::new).skipVanillaRender(be -> false).apply();
            SimpleBlockEntityVisualizer.builder(ModBlockEntities.SMART_PACKAGER.get())
                    .factory(PackagerVisual::new).skipVanillaRender(be -> false).apply();
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.SMART_FROGPORT.get(), RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.SMART_PACKAGER.get(), RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.SMART_STOCK_TICKER.get(), RenderType.cutoutMipped());
        });
    }
}
