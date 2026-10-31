package io.github.iksanbro.smartlogistics.mixin;

import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorPackage;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = ChainConveyorBlockEntity.class, remap = false)
public interface ChainConveyorBlockEntityAccessor {
    @Accessor("loopingPackages")
    List<ChainConveyorPackage> smartlogistics$looping();

    @Accessor("travellingPackages")
    Map<BlockPos, List<ChainConveyorPackage>> smartlogistics$travelling();
}
