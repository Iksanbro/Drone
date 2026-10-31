package io.github.iksanbro.smartlogistics.mixin;

import com.tterrag.registrate.util.entry.BlockEntityEntry;
import io.github.iksanbro.smartlogistics.Aliases;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Create's "is this X" check through its registry entry also accepts the smart version of X. */
@Mixin(value = BlockEntityEntry.class, remap = false)
public abstract class BlockEntityEntryMixin {
    @Inject(method = "is", at = @At("RETURN"), cancellable = true)
    private void smartlogistics$alias(BlockEntity be, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && be != null && Aliases.isAliasBlockEntity(this, be)) cir.setReturnValue(true);
    }
}
