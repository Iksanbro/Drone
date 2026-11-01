package io.github.iksanbro.smartlogistics.mixin;

import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * A recipe with more than 9 kinds of ingredients (big mechanical crafting recipes) can be ordered.
 * Create trims the ingredients of an ordered recipe to the 9 order slots of the menu, so such a
 * recipe could never be ordered. The order list shows the first 9 items and "[+n]" for the rest.
 */
@Mixin(value = StockKeeperRequestScreen.class, remap = false)
public abstract class StockKeeperOrderLimitMixin {
    @ModifyArg(method = "requestCraftable", index = 3, at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/content/logistics/stockTicker/StockKeeperRequestScreen;maxCraftable(Lcom/simibubi/create/content/logistics/stockTicker/CraftableBigItemStack;Lcom/simibubi/create/content/logistics/packager/InventorySummary;Ljava/util/function/Function;I)Lnet/createmod/catnip/data/Pair;"))
    private int smartlogistics$noTypeLimit(int newTypeLimit) {
        return -1;
    }
}
