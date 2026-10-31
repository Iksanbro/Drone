package io.github.iksanbro.smartlogistics.network;

import io.github.iksanbro.smartlogistics.SmartLogistics;
import io.github.iksanbro.smartlogistics.frogport.SmartFrogportBlockEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record FrogportSettingsPayload(BlockPos pos, List<String> addresses, int mode) implements CustomPacketPayload {
    public static final Type<FrogportSettingsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(SmartLogistics.MOD_ID, "frogport_settings"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FrogportSettingsPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, FrogportSettingsPayload::pos,
            ByteBufCodecs.stringUtf8(64).apply(ByteBufCodecs.list(16)), FrogportSettingsPayload::addresses,
            ByteBufCodecs.VAR_INT, FrogportSettingsPayload::mode,
            FrogportSettingsPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(FrogportSettingsPayload p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> Payloads.onBlockEntity((ServerPlayer) ctx.player(), p.pos, SmartFrogportBlockEntity.class,
                (player, be) -> be.applySettings(p.addresses, SmartFrogportBlockEntity.Mode.byId(p.mode))));
    }
}
