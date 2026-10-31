package io.github.iksanbro.smartlogistics.mixin;

import com.simibubi.create.content.logistics.box.PackageItem;
import io.github.iksanbro.smartlogistics.frogport.AddressList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A filter with several addresses matches a box when one of its addresses does. */
@Mixin(value = PackageItem.class, remap = false)
public abstract class PackageItemMixin {
    @Shadow
    public static boolean matchAddress(String boxAddress, String address) {
        throw new AssertionError();
    }

    @Inject(method = "matchAddress(Ljava/lang/String;Ljava/lang/String;)Z", at = @At("HEAD"), cancellable = true)
    private static void smartlogistics$severalAddresses(String boxAddress, String address, CallbackInfoReturnable<Boolean> cir) {
        if (!AddressList.isCombined(address)) return;
        for (String part : AddressList.decode(address)) {
            if (matchAddress(boxAddress, part)) {
                cir.setReturnValue(true);
                return;
            }
        }
        cir.setReturnValue(false);
    }
}
