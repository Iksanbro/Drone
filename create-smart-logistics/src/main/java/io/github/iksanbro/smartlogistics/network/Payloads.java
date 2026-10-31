package io.github.iksanbro.smartlogistics.network;

import io.github.iksanbro.smartlogistics.SmartLogistics;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class Payloads {
    private Payloads() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(SmartLogistics.MOD_ID).versioned("1");
        registrar.playToServer(FrogportSettingsPayload.TYPE, FrogportSettingsPayload.CODEC, FrogportSettingsPayload::handle);
        registrar.playToServer(PackagerSettingsPayload.TYPE, PackagerSettingsPayload.CODEC, PackagerSettingsPayload::handle);
        registrar.playToServer(TickerSettingsPayload.TYPE, TickerSettingsPayload.CODEC, TickerSettingsPayload::handle);
        registrar.playToServer(ProcessingOrderPayload.TYPE, ProcessingOrderPayload.CODEC, ProcessingOrderPayload::handle);
        registrar.playToServer(GaugeGridPayload.TYPE, GaugeGridPayload.CODEC, GaugeGridPayload::handle);
    }

    /** Runs {@code action} on the block entity at {@code pos} if the player may use it. */
    public static <T extends BlockEntity> void onBlockEntity(ServerPlayer player, BlockPos pos, Class<T> type, BiConsumer<ServerPlayer, T> action) {
        if (player.level().isLoaded(pos) && player.distanceToSqr(pos.getCenter()) < 20 * 20
                && type.isInstance(player.level().getBlockEntity(pos)) && player.mayBuild())
            action.accept(player, type.cast(player.level().getBlockEntity(pos)));
    }
}
