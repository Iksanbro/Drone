package io.github.iksanbro.cbcparts.medium;

import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import rbasamoyai.createbigcannons.munitions.big_cannon.grapeshot.GrapeshotBurstRenderer;
import rbasamoyai.createbigcannons.munitions.big_cannon.shrapnel.ShrapnelBurstRenderer;
import riftyboi.cbcmodernwarfare.munitions.medium_cannon.AbstractMediumcannonProjectile;
import riftyboi.cbcmodernwarfare.munitions.medium_cannon.MediumcannonProjectileRenderer;

/** Renderers: medium rounds are drawn as their round item, as Neo Warfare draws its own. */
final class MediumShellsClient {
    private MediumShellsClient() {}

    static void init(IEventBus modBus) {
        modBus.addListener(MediumShellsClient::onRegisterRenderers);
    }

    @SuppressWarnings("unchecked")
    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        for (MediumShells.Entry e : MediumShells.entries()) {
            event.registerEntityRenderer((EntityType<AbstractMediumcannonProjectile>) e.entity().get(),
                    MediumcannonProjectileRenderer::new);
        }
        event.registerEntityRenderer(MediumShells.SHRAPNEL_BURST.get(), ShrapnelBurstRenderer::new);
        event.registerEntityRenderer(MediumShells.GRAPESHOT_BURST.get(), GrapeshotBurstRenderer::new);
    }
}
