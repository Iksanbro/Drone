package io.github.iksanbro.smartlogistics.frogport;

import com.simibubi.create.content.logistics.packagePort.frogport.FrogportBlock;
import com.simibubi.create.content.logistics.packagePort.frogport.FrogportBlockEntity;
import io.github.iksanbro.smartlogistics.registry.ModBlockEntities;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class SmartFrogportBlock extends FrogportBlock {
    public SmartFrogportBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntityType<? extends FrogportBlockEntity> getBlockEntityType() {
        return ModBlockEntities.SMART_FROGPORT.get();
    }
}
