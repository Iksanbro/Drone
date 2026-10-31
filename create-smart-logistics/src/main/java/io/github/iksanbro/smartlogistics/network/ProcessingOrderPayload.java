package io.github.iksanbro.smartlogistics.network;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.packagerLink.WiFiEffectPacket;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import io.github.iksanbro.smartlogistics.SmartLogistics;
import io.github.iksanbro.smartlogistics.ticker.SmartStockTickerBlockEntity;
import io.github.iksanbro.smartlogistics.ticker.SmartStockTickerBlockEntity.ProcessingEntry;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * A stock keeper order sent to a smart stock ticker, with the processing recipes it contains (the
 * order Create sends has no room for them). Replaces Create's order packet for that ticker.
 */
public record ProcessingOrderPayload(BlockPos pos, PackageOrderWithCrafts order, String address, List<ProcessingEntry> processing)
        implements CustomPacketPayload {

    public static final StreamCodec<RegistryFriendlyByteBuf, ProcessingEntry> ENTRY_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(128), ProcessingEntry::recipeType,
            BigItemStack.STREAM_CODEC.apply(ByteBufCodecs.list(64)), ProcessingEntry::ingredients,
            ProcessingEntry::new);

    public static final Type<ProcessingOrderPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(SmartLogistics.MOD_ID, "processing_order"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ProcessingOrderPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ProcessingOrderPayload::pos,
            PackageOrderWithCrafts.STREAM_CODEC, ProcessingOrderPayload::order,
            ByteBufCodecs.STRING_UTF8, ProcessingOrderPayload::address,
            ENTRY_CODEC.apply(ByteBufCodecs.list(64)), ProcessingOrderPayload::processing,
            ProcessingOrderPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ProcessingOrderPayload p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> Payloads.onBlockEntity((ServerPlayer) ctx.player(), p.pos, SmartStockTickerBlockEntity.class, (player, be) -> {
            if (p.order.isEmpty()) return;
            // the same effects as Create's order packet
            AllSoundEvents.STOCK_TICKER_REQUEST.playOnServer(be.getLevel(), p.pos);
            AllAdvancements.STOCK_TICKER.awardTo(player);
            WiFiEffectPacket.send(player.level(), p.pos);
            be.broadcastWithProcessing(p.order, p.address, p.processing);
        }));
    }
}
