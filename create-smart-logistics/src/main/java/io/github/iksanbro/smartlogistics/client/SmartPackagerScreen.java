package io.github.iksanbro.smartlogistics.client;

import io.github.iksanbro.smartlogistics.network.PackagerSettingsPayload;
import io.github.iksanbro.smartlogistics.packager.SmartPackagerBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** Address of the boxes and the auto-packaging switch of a smart packager. */
public class SmartPackagerScreen extends Screen {
    private final SmartPackagerBlockEntity be;
    private EditBox address;
    private boolean autoPack;
    private Button autoButton;

    public SmartPackagerScreen(SmartPackagerBlockEntity be) {
        super(Component.translatable("block.smartlogistics.smart_packager"));
        this.be = be;
        this.autoPack = be.autoPack;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int y = height / 2 - 50;
        address = new EditBox(font, cx - 100, y + 20, 200, 18, Component.translatable("smartlogistics.gui.packager.address"));
        address.setMaxLength(48);
        address.setValue(be.customAddress);
        address.setHint(Component.translatable("smartlogistics.gui.packager.address_hint"));
        addRenderableWidget(address);
        setInitialFocus(address);

        autoButton = Button.builder(autoText(), b -> {
            autoPack = !autoPack;
            b.setMessage(autoText());
        }).bounds(cx - 100, y + 50, 200, 20).build();
        addRenderableWidget(autoButton);

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(cx - 50, y + 80, 100, 20).build());
    }

    private Component autoText() {
        return Component.translatable(autoPack ? "smartlogistics.gui.packager.auto_on" : "smartlogistics.gui.packager.auto_off");
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        int cx = width / 2;
        int y = height / 2 - 50;
        graphics.fill(cx - 110, y - 14, cx + 110, y + 108, 0xC0101010);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int cx = width / 2;
        int y = height / 2 - 50;
        graphics.drawCenteredString(font, title, cx, y - 6, 0xFFFFFF);
        graphics.drawString(font, Component.translatable("smartlogistics.gui.packager.address"), cx - 100, y + 9, 0xA0A0A0);
        if (autoButton.isHovered())
            graphics.renderTooltip(font, font.split(Component.translatable("smartlogistics.gui.packager.auto_tooltip"), 220), mouseX, mouseY);
    }

    @Override
    public void onClose() {
        PacketDistributor.sendToServer(new PackagerSettingsPayload(be.getBlockPos(), address.getValue(), autoPack));
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
