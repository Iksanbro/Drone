package io.github.iksanbro.smartlogistics.client;

import io.github.iksanbro.smartlogistics.packager.SmartPackagerBlockEntity;
import io.github.iksanbro.smartlogistics.ticker.SmartStockTickerBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/** Client-only calls from common code; only reached on the client. */
public final class ClientHooks {
    private ClientHooks() {}

    public static void openPackagerSettings(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.getBlockEntity(pos) instanceof SmartPackagerBlockEntity be)
            mc.setScreen(new SmartPackagerScreen(be));
    }

    public static void openTickerSettings(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.getBlockEntity(pos) instanceof SmartStockTickerBlockEntity be)
            mc.setScreen(new TickerSettingsScreen(be));
    }
}
