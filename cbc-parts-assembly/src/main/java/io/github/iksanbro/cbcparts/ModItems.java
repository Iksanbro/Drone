package io.github.iksanbro.cbcparts;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    private ModItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CBCPartsAssembly.MOD_ID);

    static void register(IEventBus modBus) {
        ModList mods = ModList.get();
        int count = 0;
        for (GeneratedParts.UnfinishedPart part : GeneratedParts.ALL) {
            // Items for addons that are not installed are skipped; their recipes are skipped too.
            if (!mods.isLoaded(part.modId())) continue;
            ResourceLocation target = ResourceLocation.fromNamespaceAndPath(part.modId(), part.targetPath());
            ITEMS.register(part.name(), () -> new UnfinishedPartItem(new Item.Properties(), target));
            count++;
        }
        ITEMS.register(modBus);
        CBCPartsAssembly.LOGGER.info("Registered {} unfinished cannon part items", count);
    }
}
