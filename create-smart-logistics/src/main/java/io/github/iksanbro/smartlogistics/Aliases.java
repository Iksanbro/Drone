package io.github.iksanbro.smartlogistics;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import io.github.iksanbro.smartlogistics.frogport.SmartFrogportBlockEntity;
import io.github.iksanbro.smartlogistics.packager.SmartPackagerBlockEntity;
import io.github.iksanbro.smartlogistics.registry.ModBlocks;
import io.github.iksanbro.smartlogistics.ticker.SmartStockTickerBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Lets Create treat the smart blocks as the blocks they improve. Create checks "is this a frogport /
 * packager / stock ticker" in many places (placing a frogport on a chain, factory gauges on a
 * packager, the stock keeper's seat...) through its registry entries; the mixins on those entries
 * ask here.
 */
public final class Aliases {
    private Aliases() {}

    private static boolean ready() {
        return ModBlocks.SMART_STOCK_TICKER.isBound();
    }

    public static boolean isAliasItem(Object entry, ItemStack stack) {
        if (!ready()) return false;
        if (entry == AllBlocks.PACKAGE_FROGPORT) return stack.is(ModBlocks.SMART_FROGPORT.get().asItem());
        if (entry == AllBlocks.PACKAGER) return stack.is(ModBlocks.SMART_PACKAGER.get().asItem());
        if (entry == AllBlocks.STOCK_TICKER) return stack.is(ModBlocks.SMART_STOCK_TICKER.get().asItem());
        return false;
    }

    public static boolean isAliasBlock(Object entry, BlockState state) {
        if (!ready()) return false;
        if (entry == AllBlocks.PACKAGE_FROGPORT) return state.is(ModBlocks.SMART_FROGPORT.get());
        if (entry == AllBlocks.PACKAGER) return state.is(ModBlocks.SMART_PACKAGER.get());
        if (entry == AllBlocks.STOCK_TICKER) return state.is(ModBlocks.SMART_STOCK_TICKER.get());
        return false;
    }

    public static boolean isAliasBlockEntity(Object entry, BlockEntity be) {
        if (entry == AllBlockEntityTypes.PACKAGE_FROGPORT) return be instanceof SmartFrogportBlockEntity;
        if (entry == AllBlockEntityTypes.PACKAGER) return be instanceof SmartPackagerBlockEntity;
        if (entry == AllBlockEntityTypes.STOCK_TICKER) return be instanceof SmartStockTickerBlockEntity;
        return false;
    }
}
