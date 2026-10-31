package io.github.iksanbro.smartlogistics.network;

import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelPosition;
import io.github.iksanbro.smartlogistics.SmartLogistics;
import io.github.iksanbro.smartlogistics.gauge.GridCodec;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * The crafting grid of a factory gauge bigger than 3 x 3. Sent after Create's own gauge settings
 * packet, which then carries an empty grid (the big one would not fit in it).
 */
public record GaugeGridPayload(FactoryPanelPosition position, List<ItemStack> palette, int[] cells) implements CustomPacketPayload {
    public static final Type<GaugeGridPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(SmartLogistics.MOD_ID, "gauge_grid"));

    private static final StreamCodec<RegistryFriendlyByteBuf, int[]> CELLS = new StreamCodec<>() {
        @Override
        public int[] decode(RegistryFriendlyByteBuf buf) {
            int n = Math.min(buf.readVarInt(), GridCodec.MAX_CELLS);
            int[] cells = new int[n];
            for (int i = 0; i < n; i++) cells[i] = buf.readVarInt();
            return cells;
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, int[] cells) {
            buf.writeVarInt(cells.length);
            for (int c : cells) buf.writeVarInt(c);
        }
    };

    public static final StreamCodec<RegistryFriendlyByteBuf, GaugeGridPayload> CODEC = StreamCodec.composite(
            FactoryPanelPosition.STREAM_CODEC, GaugeGridPayload::position,
            ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list(256)), GaugeGridPayload::palette,
            CELLS, GaugeGridPayload::cells,
            GaugeGridPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(GaugeGridPayload p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> Payloads.onBlockEntity((ServerPlayer) ctx.player(), p.position.pos(), FactoryPanelBlockEntity.class, (player, be) -> {
            FactoryPanelBehaviour panel = be.panels.get(p.position.slot());
            if (panel == null || GridCodec.side(p.cells.length) == 0) return;
            panel.activeCraftingArrangement = GridCodec.decode(p.palette, p.cells);
            be.notifyUpdate();
        }));
    }
}
