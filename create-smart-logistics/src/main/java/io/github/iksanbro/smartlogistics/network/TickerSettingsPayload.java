package io.github.iksanbro.smartlogistics.network;

import io.github.iksanbro.smartlogistics.SmartLogistics;
import io.github.iksanbro.smartlogistics.ticker.SmartStockTickerBlockEntity;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record TickerSettingsPayload(BlockPos pos, String craftingAddress, Map<String, String> typeAddresses) implements CustomPacketPayload {
    public static final Type<TickerSettingsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(SmartLogistics.MOD_ID, "ticker_settings"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TickerSettingsPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, TickerSettingsPayload::pos,
            ByteBufCodecs.stringUtf8(64), TickerSettingsPayload::craftingAddress,
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.stringUtf8(128), ByteBufCodecs.stringUtf8(64), 512), TickerSettingsPayload::typeAddresses,
            TickerSettingsPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TickerSettingsPayload p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> Payloads.onBlockEntity((ServerPlayer) ctx.player(), p.pos, SmartStockTickerBlockEntity.class,
                (player, be) -> be.applySettings(p.craftingAddress, p.typeAddresses)));
    }
}
