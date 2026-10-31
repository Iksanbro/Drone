package io.github.iksanbro.smartlogistics.mixin;

import com.tterrag.registrate.util.entry.BlockEntry;
import io.github.iksanbro.smartlogistics.Aliases;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Create's "is this X" check through its registry entry also accepts the smart version of X. */
@Mixin(value = BlockEntry.class, remap = false)
public abstract class BlockEntryMixin {
    @Inject(method = "has", at = @At("RETURN"), cancellable = true)
    private void smartlogistics$alias(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && Aliases.isAliasBlock(this, state)) cir.setReturnValue(true);
    }
}
