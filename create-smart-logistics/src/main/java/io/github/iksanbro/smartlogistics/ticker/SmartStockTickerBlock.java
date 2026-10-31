package io.github.iksanbro.smartlogistics.ticker;

import com.simibubi.create.content.logistics.stockTicker.StockTickerBlock;
import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;
import io.github.iksanbro.smartlogistics.client.ClientHooks;
import io.github.iksanbro.smartlogistics.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.fml.loading.FMLEnvironment;

public class SmartStockTickerBlock extends StockTickerBlock {
    public SmartStockTickerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntityType<? extends StockTickerBlockEntity> getBlockEntityType() {
        return ModBlockEntities.SMART_STOCK_TICKER.get();
    }

    /** Sneak + right click with an empty hand opens the address settings. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hitResult) {
        if (stack.isEmpty() && player.isShiftKeyDown()) {
            if (level.isClientSide() && FMLEnvironment.dist.isClient()) ClientHooks.openTickerSettings(pos);
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }
}
