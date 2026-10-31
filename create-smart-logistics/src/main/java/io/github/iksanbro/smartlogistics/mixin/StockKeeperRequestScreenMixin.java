package io.github.iksanbro.smartlogistics.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.logistics.AddressEditBox;
import com.simibubi.create.content.logistics.stockTicker.CraftableBigItemStack;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen;
import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;
import io.github.iksanbro.smartlogistics.network.ProcessingOrderPayload;
import io.github.iksanbro.smartlogistics.ticker.SmartStockTickerBlockEntity;
import io.github.iksanbro.smartlogistics.ticker.TickerOrders;
import java.util.List;
import net.createmod.catnip.platform.services.NetworkHelper;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * An order placed at a smart stock ticker goes out as {@link ProcessingOrderPayload}, which also
 * carries the processing recipes (crushing etc.) picked through JEI or EMI, so the ticker can send
 * their ingredients to the address of their recipe type.
 */
@Mixin(value = StockKeeperRequestScreen.class, remap = false)
public abstract class StockKeeperRequestScreenMixin {
    @Shadow
    StockTickerBlockEntity blockEntity;
    @Shadow
    public AddressEditBox addressBox;
    @Shadow
    public List<CraftableBigItemStack> recipesToOrder;
    @Shadow
    boolean encodeRequester;

    @WrapOperation(method = "sendIt", at = @At(value = "INVOKE",
            target = "Lnet/createmod/catnip/platform/services/NetworkHelper;sendToServer(Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload;)V"))
    private void smartlogistics$sendWithProcessing(NetworkHelper network, CustomPacketPayload packet, Operation<Void> original,
                                                   @Local PackageOrderWithCrafts order) {
        if (!(blockEntity instanceof SmartStockTickerBlockEntity) || encodeRequester) {
            original.call(network, packet);
            return;
        }
        PacketDistributor.sendToServer(new ProcessingOrderPayload(blockEntity.getBlockPos(), order, addressBox.getValue(),
                TickerOrders.processingEntries(recipesToOrder, order, blockEntity.getLevel())));
    }
}
