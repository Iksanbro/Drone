package io.github.iksanbro.smartlogistics.mixin;

import com.tterrag.registrate.util.entry.ItemProviderEntry;
import io.github.iksanbro.smartlogistics.Aliases;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Create's "is this X" check through its registry entry also accepts the smart version of X. */
@Mixin(value = ItemProviderEntry.class, remap = false)
public abstract class ItemProviderEntryMixin {
    @Inject(method = "isIn", at = @At("RETURN"), cancellable = true)
    private void smartlogistics$alias(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && Aliases.isAliasItem(this, stack)) cir.setReturnValue(true);
    }
}
