package io.github.iksanbro.smartlogistics.client;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.content.logistics.packagePort.PackagePortMenu;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.widget.IconButton;
import io.github.iksanbro.smartlogistics.frogport.AddressList;
import io.github.iksanbro.smartlogistics.frogport.SmartFrogportBlockEntity;
import io.github.iksanbro.smartlogistics.frogport.SmartFrogportBlockEntity.Mode;
import io.github.iksanbro.smartlogistics.network.FrogportSettingsPayload;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/** The frogport's package slots and send / receive mode; its addresses are in a tab beside it. */
public class SmartFrogportScreen extends AbstractSimiContainerScreen<PackagePortMenu> {
    /** the tab stays as the player left it */
    private static boolean panelOpen = true;

    private final AllGuiTextures background = AllGuiTextures.FROGPORT_BG;
    private final IconButton[] modeButtons = new IconButton[3];
    private Mode mode;
    private final ItemStack icon;
    private AddressPanel panel;
    private List<Rect2i> extraAreas = Collections.emptyList();

    public SmartFrogportScreen(PackagePortMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        icon = new ItemStack(menu.contentHolder.getBlockState().getBlock().asItem());
        mode = menu.contentHolder instanceof SmartFrogportBlockEntity port ? port.mode : Mode.BOTH;
    }

    private SmartFrogportBlockEntity port() {
        return menu.contentHolder instanceof SmartFrogportBlockEntity port ? port : null;
    }

    @Override
    protected void init() {
        setWindowSize(background.getWidth(), background.getHeight() + AllGuiTextures.PLAYER_INVENTORY.getHeight());
        super.init();
        clearWidgets();
        int x = getGuiLeft();
        int y = getGuiTop();

        if (panel == null) {
            List<String> current = new ArrayList<>();
            SmartFrogportBlockEntity port = port();
            if (port != null) current.addAll(port.addresses.isEmpty() && !port.addressFilter.isBlank() ? List.of(port.addressFilter) : port.addresses);
            panel = AddressPanel.addresses(font, Component.translatable("smartlogistics.gui.frogport.addresses"), current);
            panel.setOpen(panelOpen);
        }
        panel.layout(x, x + background.getWidth(), y - 17, width, height);

        IconButton confirm = new IconButton(x + background.getWidth() - 33, y + background.getHeight() - 24, AllIcons.I_CONFIRM);
        confirm.withCallback(() -> minecraft.player.closeContainer());
        addRenderableWidget(confirm);

        AllIcons[] icons = {AllIcons.I_SEND_AND_RECEIVE, AllIcons.I_SEND_ONLY, AllIcons.I_PASSIVE};
        for (int i = 0; i < 3; i++) {
            Mode m = Mode.values()[i];
            IconButton b = new IconButton(x + 37 + i * 18, y + background.getHeight() - 24, icons[i]);
            b.withCallback(() -> {
                mode = m;
                updateModeButtons();
            });
            b.setToolTip(Component.translatable("smartlogistics.gui.frogport.mode." + m.name().toLowerCase(Locale.ROOT)));
            modeButtons[i] = b;
            addRenderableWidget(b);
        }
        updateModeButtons();
    }

    private void updateModeButtons() {
        for (int i = 0; i < 3; i++) modeButtons[i].green = mode.ordinal() == i;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = getGuiLeft();
        int y = getGuiTop();
        AllGuiTextures.FROGPORT_HEADER.render(graphics, x, y - AllGuiTextures.FROGPORT_HEADER.getHeight());
        background.render(graphics, x, y);
        Component name = icon.getHoverName();
        graphics.drawString(font, name, x + background.getWidth() / 2 - font.width(name) / 2, y - 11, 0x3D3C48, false);
        GuiGameElement.of(icon).<GuiGameElement.GuiRenderBuilder>at(x + 8, y + background.getHeight() - 34, -200)
                .scale(1.5).render(graphics);
        renderPlayerInventory(graphics, leftPos + 30, topPos + 8 + imageHeight - AllGuiTextures.PLAYER_INVENTORY.getHeight());

        SmartFrogportBlockEntity port = port();
        if (port != null && port.target != null) {
            AllGuiTextures.FROGPORT_SLOT.render(graphics, x + 13, y + 58);
            graphics.renderItem(port.target.getIcon(), x + 14, y + 59);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);
        panel.render(graphics, mouseX, mouseY, partialTicks);
        panelOpen = panel.isOpen();
        extraAreas = ImmutableList.of(panel.area());
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
        SmartFrogportBlockEntity port = port();
        // addresses only when changed here, so an out of date screen does not overwrite them
        List<String> addresses = panel.isEdited() || port == null ? AddressList.clean(panel.addressValues()) : port.addresses;
        PacketDistributor.sendToServer(new FrogportSettingsPayload(menu.contentHolder.getBlockPos(), addresses, mode.ordinal()));
        super.removed();
    }

    @Override
    public List<Rect2i> getExtraAreas() {
        return extraAreas;
    }
}
