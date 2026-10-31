package io.github.iksanbro.smartlogistics.network;

import io.github.iksanbro.smartlogistics.SmartLogistics;
import io.github.iksanbro.smartlogistics.packager.SmartPackagerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PackagerSettingsPayload(BlockPos pos, String address, boolean autoPack) implements CustomPacketPayload {
    public static final Type<PackagerSettingsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(SmartLogistics.MOD_ID, "packager_settings"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PackagerSettingsPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, PackagerSettingsPayload::pos,
            ByteBufCodecs.stringUtf8(64), PackagerSettingsPayload::address,
            ByteBufCodecs.BOOL, PackagerSettingsPayload::autoPack,
            PackagerSettingsPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PackagerSettingsPayload p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> Payloads.onBlockEntity((ServerPlayer) ctx.player(), p.pos, SmartPackagerBlockEntity.class,
                (player, be) -> be.applySettings(p.address, p.autoPack)));
    }
}
