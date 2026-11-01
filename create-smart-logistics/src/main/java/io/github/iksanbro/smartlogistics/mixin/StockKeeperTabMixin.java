package io.github.iksanbro.smartlogistics.mixin;

import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestMenu;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen;
import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import io.github.iksanbro.smartlogistics.client.AddressPanel;
import io.github.iksanbro.smartlogistics.client.TabHolder;
import io.github.iksanbro.smartlogistics.client.TickerPanels;
import io.github.iksanbro.smartlogistics.ticker.SmartStockTickerBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The address tab of a smart stock ticker in the stock keeper's order menu. The menu handles clicks
 * and keys itself, so they go to the tab first.
 */
@Mixin(value = StockKeeperRequestScreen.class, remap = false)
public abstract class StockKeeperTabMixin extends AbstractSimiContainerScreen<StockKeeperRequestMenu> implements TabHolder {
    @Unique
    private static boolean smartlogistics$tabOpen;

    @Shadow
    StockTickerBlockEntity blockEntity;

    @Unique
    private AddressPanel smartlogistics$panel;

    @Override
    public AddressPanel smartlogistics$panel() {
        return smartlogistics$panel;
    }

    protected StockKeeperTabMixin(StockKeeperRequestMenu container, Inventory inv, Component title) {
        super(container, inv, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void smartlogistics$initTab(CallbackInfo ci) {
        if (!(blockEntity instanceof SmartStockTickerBlockEntity smart)) return;
        if (smartlogistics$panel == null) {
            smartlogistics$panel = TickerPanels.create(smart, font);
            smartlogistics$panel.setOpen(smartlogistics$tabOpen);
        }
        smartlogistics$panel.layout(getGuiLeft(), getGuiLeft() + imageWidth, getGuiTop() + 20, width, height);
    }

    @Inject(method = "renderForeground", at = @At("TAIL"))
    private void smartlogistics$renderTab(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (smartlogistics$panel == null) return;
        smartlogistics$panel.render(graphics, mouseX, mouseY, partialTicks);
        if (smartlogistics$tabOpen && !smartlogistics$panel.isOpen() && blockEntity instanceof SmartStockTickerBlockEntity smart)
            TickerPanels.save(smart, smartlogistics$panel); // closing the tab saves
        smartlogistics$tabOpen = smartlogistics$panel.isOpen();
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void smartlogistics$clickTab(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (smartlogistics$panel != null && smartlogistics$panel.mouseClicked(mouseX, mouseY, button)) cir.setReturnValue(true);
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void smartlogistics$scrollTab(double mouseX, double mouseY, double scrollX, double scrollY, CallbackInfoReturnable<Boolean> cir) {
        if (smartlogistics$panel != null && smartlogistics$panel.mouseScrolled(mouseX, mouseY, scrollY)) cir.setReturnValue(true);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void smartlogistics$keyTab(int key, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (smartlogistics$panel != null && smartlogistics$panel.keyPressed(key, scanCode, modifiers)) cir.setReturnValue(true);
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void smartlogistics$typeTab(char c, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (smartlogistics$panel != null && smartlogistics$panel.charTyped(c, modifiers)) cir.setReturnValue(true);
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void smartlogistics$saveTab(CallbackInfo ci) {
        if (smartlogistics$panel != null && blockEntity instanceof SmartStockTickerBlockEntity smart)
            TickerPanels.save(smart, smartlogistics$panel);
    }

    /** JEI keeps its item list off the tab. */
    @Inject(method = "getExtraAreas", at = @At("RETURN"), cancellable = true)
    private void smartlogistics$tabArea(CallbackInfoReturnable<List<Rect2i>> cir) {
        if (smartlogistics$panel == null) return;
        List<Rect2i> areas = new ArrayList<>(cir.getReturnValue());
        areas.add(smartlogistics$panel.area());
        cir.setReturnValue(areas);
    }
}
