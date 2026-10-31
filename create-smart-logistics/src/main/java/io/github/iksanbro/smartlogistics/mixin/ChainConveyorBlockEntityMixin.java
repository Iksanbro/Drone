package io.github.iksanbro.smartlogistics.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorPackage;
import com.simibubi.create.infrastructure.config.AllConfigs;
import io.github.iksanbro.smartlogistics.Config;
import io.github.iksanbro.smartlogistics.frogport.SmartFrogportBlockEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Chain conveyor jams.
 *
 * <p>A package looping on a conveyor moves on to the next conveyor only if that one holds fewer
 * looping packages than its capacity (20). When two neighbours are both full and their packages
 * want to cross over, each waits for the other forever, and every package behind them waits too:
 * the network stops. Here a conveyor takes packages from a neighbour up to a few times its capacity,
 * and always when it gives one back in exchange. Frogports still put packages on only below the
 * capacity, which now counts every package on the conveyor (Create counted the travelling packages
 * per connection instead of one by one).
 *
 * <p>Also: a smart frogport catches several packages at once.
 */
@Mixin(value = ChainConveyorBlockEntity.class, remap = false)
public abstract class ChainConveyorBlockEntityMixin {

    @WrapOperation(method = "tick", at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/content/kinetics/chainConveyor/ChainConveyorBlockEntity;canAcceptMorePackagesFromOtherConveyor()Z"))
    private boolean smartlogistics$acceptFromNeighbour(ChainConveyorBlockEntity receiver, Operation<Boolean> original) {
        if (original.call(receiver)) return true;
        if (!Config.CHAIN_JAM_FIX.get()) return false;
        ChainConveyorBlockEntity self = (ChainConveyorBlockEntity) (Object) this;
        List<ChainConveyorPackage> looping = ((ChainConveyorBlockEntityAccessor) receiver).smartlogistics$looping();
        int capacity = AllConfigs.server().logistics.chainConveyorCapacity.get();
        if (looping.size() < Math.ceil(capacity * Config.CHAIN_OVERFLOW.get())) return true;
        // a full neighbour still takes the package if one of its own heads back here: they swap
        BlockPos back = self.getBlockPos().subtract(receiver.getBlockPos());
        for (ChainConveyorPackage box : looping)
            if (back.equals(receiver.routingTable.getExitFor(box.item))) return true;
        return false;
    }

    @Inject(method = "canAcceptMorePackages", at = @At("HEAD"), cancellable = true)
    private void smartlogistics$countEveryPackage(CallbackInfoReturnable<Boolean> cir) {
        if (!Config.CHAIN_JAM_FIX.get()) return;
        ChainConveyorBlockEntityAccessor self = (ChainConveyorBlockEntityAccessor) this;
        int count = self.smartlogistics$looping().size();
        for (List<ChainConveyorPackage> list : self.smartlogistics$travelling().values()) count += list.size();
        cir.setReturnValue(count < AllConfigs.server().logistics.chainConveyorCapacity.get());
    }

    @Inject(method = "exportToPort", at = @At("HEAD"), cancellable = true)
    private void smartlogistics$smartFrogportCatch(ChainConveyorPackage box, BlockPos offset, CallbackInfoReturnable<Boolean> cir) {
        ChainConveyorBlockEntity self = (ChainConveyorBlockEntity) (Object) this;
        if (self.getLevel().getBlockEntity(self.getBlockPos().offset(offset)) instanceof SmartFrogportBlockEntity port)
            cir.setReturnValue(port.tryCatch(box.item));
    }
}
