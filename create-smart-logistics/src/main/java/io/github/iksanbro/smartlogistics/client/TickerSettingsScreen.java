package io.github.iksanbro.smartlogistics.client;

import io.github.iksanbro.smartlogistics.ticker.SmartStockTickerBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The smart stock ticker's address panel on its own (sneak + right click on the ticker), for when
 * no stock keeper sits at it. With a keeper the same panel is a tab of the order menu.
 */
public class TickerSettingsScreen extends Screen {
    private final SmartStockTickerBlockEntity be;
    private AddressPanel panel;

    public TickerSettingsScreen(SmartStockTickerBlockEntity be) {
        super(Component.translatable("block.smartlogistics.smart_stock_ticker"));
        this.be = be;
    }

    @Override
    protected void init() {
        if (panel == null) panel = TickerPanels.create(be, font).alwaysOpen();
        panel.layout(width / 2, width / 2, height / 2, width, height);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        panel.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return panel.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return panel.mouseScrolled(mouseX, mouseY, scrollY) || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return panel.keyPressed(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        return panel.charTyped(c, modifiers) || super.charTyped(c, modifiers);
    }

    @Override
    public void removed() {
        TickerPanels.save(be, panel);
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
