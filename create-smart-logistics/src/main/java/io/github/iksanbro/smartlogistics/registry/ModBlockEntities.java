package io.github.iksanbro.smartlogistics.registry;

import io.github.iksanbro.smartlogistics.SmartLogistics;
import io.github.iksanbro.smartlogistics.frogport.SmartFrogportBlockEntity;
import io.github.iksanbro.smartlogistics.packager.SmartPackagerBlockEntity;
import io.github.iksanbro.smartlogistics.ticker.SmartStockTickerBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    private ModBlockEntities() {}

    public static final DeferredRegister<BlockEntityType<?>> TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, SmartLogistics.MOD_ID);

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SmartFrogportBlockEntity>> SMART_FROGPORT = TYPES.register("smart_frogport",
            () -> BlockEntityType.Builder.of(SmartFrogportBlockEntity::new, ModBlocks.SMART_FROGPORT.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SmartPackagerBlockEntity>> SMART_PACKAGER = TYPES.register("smart_packager",
            () -> BlockEntityType.Builder.of(SmartPackagerBlockEntity::new, ModBlocks.SMART_PACKAGER.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SmartStockTickerBlockEntity>> SMART_STOCK_TICKER = TYPES.register("smart_stock_ticker",
            () -> BlockEntityType.Builder.of(SmartStockTickerBlockEntity::new, ModBlocks.SMART_STOCK_TICKER.get()).build(null));

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, SMART_FROGPORT.get(), (be, side) -> be.automationHandler());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, SMART_PACKAGER.get(), (be, side) -> be.smartInventory);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, SMART_STOCK_TICKER.get(), (be, side) -> be.payments());
    }
}
