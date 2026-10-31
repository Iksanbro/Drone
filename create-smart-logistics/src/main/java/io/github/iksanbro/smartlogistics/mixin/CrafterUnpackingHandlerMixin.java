package io.github.iksanbro.smartlogistics.mixin;

import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.impl.unpacking.CrafterUnpackingHandler;
import io.github.iksanbro.smartlogistics.Config;
import io.github.iksanbro.smartlogistics.crafter.CrafterUnpacking;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Mechanical crafters unpack crafting boxes with {@link CrafterUnpacking}. */
@Mixin(value = CrafterUnpackingHandler.class, remap = false)
public abstract class CrafterUnpackingHandlerMixin {
    @Inject(method = "unpack", at = @At("HEAD"), cancellable = true)
    private void smartlogistics$byGridPlace(Level level, BlockPos pos, BlockState state, Direction side, List<ItemStack> items,
                                            PackageOrderWithCrafts orderContext, boolean simulate, CallbackInfoReturnable<Boolean> cir) {
        if (Config.CRAFTER_FIX.get())
            cir.setReturnValue(CrafterUnpacking.unpack(level, pos, state, side, items, orderContext, simulate));
    }
}
