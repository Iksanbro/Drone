package io.github.iksanbro.smartlogistics.registry;

import com.simibubi.create.content.logistics.packagePort.PackagePortItem;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBlockItem;
import io.github.iksanbro.smartlogistics.SmartLogistics;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    private ModItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SmartLogistics.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SmartLogistics.MOD_ID);

    // the frogport item asks the client for the chain it was aimed at, like Create's frogport
    public static final DeferredItem<PackagePortItem> SMART_FROGPORT = ITEMS.register("smart_frogport",
            () -> new PackagePortItem(ModBlocks.SMART_FROGPORT.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> SMART_PACKAGER = ITEMS.register("smart_packager",
            () -> new BlockItem(ModBlocks.SMART_PACKAGER.get(), new Item.Properties()));
    // linked to a logistics network on placement, like Create's stock ticker
    public static final DeferredItem<LogisticallyLinkedBlockItem> SMART_STOCK_TICKER = ITEMS.register("smart_stock_ticker",
            () -> new LogisticallyLinkedBlockItem(ModBlocks.SMART_STOCK_TICKER.get(), new Item.Properties()));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.smartlogistics"))
            .icon(() -> SMART_FROGPORT.get().getDefaultInstance())
            .displayItems((params, output) -> {
                output.accept(SMART_FROGPORT.get());
                output.accept(SMART_PACKAGER.get());
                output.accept(SMART_STOCK_TICKER.get());
            })
            .build());
}
