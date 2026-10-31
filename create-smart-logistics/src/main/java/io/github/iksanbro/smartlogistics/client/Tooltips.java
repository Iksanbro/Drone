package io.github.iksanbro.smartlogistics.client;

import io.github.iksanbro.smartlogistics.frogport.SmartFrogportBlockEntity;
import io.github.iksanbro.smartlogistics.packager.SmartPackagerBlockEntity;
import io.github.iksanbro.smartlogistics.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

public final class Tooltips {
    private Tooltips() {}

    public static void onTooltip(ItemTooltipEvent event) {
        Item item = event.getItemStack().getItem();
        Component line = null;
        if (item == ModItems.SMART_FROGPORT.get())
            line = Component.translatable("smartlogistics.tooltip.smart_frogport", batch(true));
        else if (item == ModItems.SMART_PACKAGER.get())
            line = Component.translatable("smartlogistics.tooltip.smart_packager", batch(false));
        else if (item == ModItems.SMART_STOCK_TICKER.get())
            line = Component.translatable("smartlogistics.tooltip.smart_stock_ticker");
        if (line != null) event.getToolTip().add(1, line.copy().withStyle(ChatFormatting.GRAY));
    }

    /** The server config is not there in the main menu: show the default then. */
    private static int batch(boolean frogport) {
        try {
            return frogport ? SmartFrogportBlockEntity.batch() : SmartPackagerBlockEntity.batch();
        } catch (IllegalStateException notLoaded) {
            return 5;
        }
    }
}
