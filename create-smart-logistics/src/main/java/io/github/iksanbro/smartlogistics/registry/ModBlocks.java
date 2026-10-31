package io.github.iksanbro.smartlogistics.registry;

import io.github.iksanbro.smartlogistics.SmartLogistics;
import io.github.iksanbro.smartlogistics.frogport.SmartFrogportBlock;
import io.github.iksanbro.smartlogistics.packager.SmartPackagerBlock;
import io.github.iksanbro.smartlogistics.ticker.SmartStockTickerBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    private ModBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(SmartLogistics.MOD_ID);

    private static BlockBehaviour.Properties softMetal() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(2f, 6f).requiresCorrectToolForDrops()
                .sound(SoundType.NETHERITE_BLOCK);
    }

    public static final DeferredBlock<SmartFrogportBlock> SMART_FROGPORT = BLOCKS.register("smart_frogport",
            () -> new SmartFrogportBlock(softMetal().noOcclusion().mapColor(MapColor.TERRACOTTA_PURPLE)));

    public static final DeferredBlock<SmartPackagerBlock> SMART_PACKAGER = BLOCKS.register("smart_packager",
            () -> new SmartPackagerBlock(softMetal().noOcclusion().mapColor(MapColor.TERRACOTTA_PURPLE)
                    .isRedstoneConductor((s, l, p) -> false)));

    public static final DeferredBlock<SmartStockTickerBlock> SMART_STOCK_TICKER = BLOCKS.register("smart_stock_ticker",
            () -> new SmartStockTickerBlock(softMetal().sound(SoundType.GLASS).mapColor(MapColor.TERRACOTTA_PURPLE)));
}
