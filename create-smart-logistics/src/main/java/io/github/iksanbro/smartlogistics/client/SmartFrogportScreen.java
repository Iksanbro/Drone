package io.github.iksanbro.smartlogistics.client;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.platform.InputConstants;
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
import net.createmod.catnip.gui.element.GuiGameElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/** The frogport's package slots, a column of addresses and the send / receive mode. */
public class SmartFrogportScreen extends AbstractSimiContainerScreen<PackagePortMenu> {
    private static final int PANEL_W = 104;
    private static final int ROW = 16;

    private final AllGuiTextures background = AllGuiTextures.FROGPORT_BG;
    private final List<EditBox> addressBoxes = new ArrayList<>();
    private final IconButton[] modeButtons = new IconButton[3];
    private Mode mode;
    private final ItemStack icon;
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
        // the address column is part of the window, so the whole thing stays centred on the screen
        setWindowSize(background.getWidth() + 4 + PANEL_W, background.getHeight() + AllGuiTextures.PLAYER_INVENTORY.getHeight());
        super.init();
        clearWidgets();
        int x = getGuiLeft();
        int y = getGuiTop();

        List<String> current = new ArrayList<>();
        SmartFrogportBlockEntity port = port();
        if (port != null) current.addAll(port.addresses.isEmpty() && !port.addressFilter.isBlank() ? List.of(port.addressFilter) : port.addresses);

        addressBoxes.clear();
        int px = x + background.getWidth() + 4;
        int py = y - 17;
        for (int i = 0; i < AddressList.MAX; i++) {
            EditBox box = new EditBox(font, px + 8, py + 20 + i * ROW, PANEL_W - 16, 12, Component.empty());
            box.setMaxLength(48);
            box.setValue(i < current.size() ? current.get(i) : "");
            box.setHint(Component.translatable("smartlogistics.gui.frogport.address_hint", i + 1));
            addressBoxes.add(box);
            addRenderableWidget(box);
        }

        IconButton confirm = new IconButton(x + background.getWidth() - 33, y + background.getHeight() - 24, AllIcons.I_CONFIRM);
        confirm.withCallback(() -> minecraft.player.closeContainer());
        addRenderableWidget(confirm);

        AllIcons[] icons = {AllIcons.I_SEND_AND_RECEIVE, AllIcons.I_SEND_ONLY, AllIcons.I_PASSIVE};
        String[] keys = {"both", "send_only", "receive_only"};
        for (int i = 0; i < 3; i++) {
            Mode m = Mode.values()[i];
            IconButton b = new IconButton(x + 37 + i * 18, y + background.getHeight() - 24, icons[i]);
            b.withCallback(() -> {
                mode = m;
                updateModeButtons();
            });
            b.setToolTip(Component.translatable("smartlogistics.gui.frogport.mode." + keys[i]));
            modeButtons[i] = b;
            addRenderableWidget(b);
        }
        updateModeButtons();

        extraAreas = ImmutableList.of(new Rect2i(px, py, PANEL_W, 24 + AddressList.MAX * ROW));
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

        // address panel
        int px = x + background.getWidth() + 4;
        int py = y - 17;
        int ph = 24 + AddressList.MAX * ROW;
        graphics.fill(px, py, px + PANEL_W, py + ph, 0xFF3D3C48);
        graphics.fill(px + 1, py + 1, px + PANEL_W - 1, py + ph - 1, 0xFFC5BFAB);
        graphics.drawString(font, Component.translatable("smartlogistics.gui.frogport.addresses"), px + 8, py + 7, 0x3D3C48, false);

        Component modeText = Component.translatable("smartlogistics.gui.frogport.mode." + mode.name().toLowerCase(java.util.Locale.ROOT));
        graphics.drawString(font, modeText, x + 37 + 3 * 18 + 6, y + background.getHeight() - 19, 0x3D3C48, false);

        SmartFrogportBlockEntity port = port();
        if (port != null && port.target != null) {
            AllGuiTextures.FROGPORT_SLOT.render(graphics, x + 13, y + 58);
            graphics.renderItem(port.target.getIcon(), x + 14, y + 59);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean enter = keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER;
        for (EditBox box : addressBoxes) {
            if (!box.isFocused()) continue;
            if (enter) {
                box.setFocused(false);
                return true;
            }
            // typing in a box must not close the screen with the inventory key
            if (keyCode != InputConstants.KEY_ESCAPE) return box.keyPressed(keyCode, scanCode, modifiers) || box.canConsumeInput();
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void removed() {
        List<String> addresses = new ArrayList<>();
        for (EditBox box : addressBoxes) addresses.add(box.getValue());
        PacketDistributor.sendToServer(new FrogportSettingsPayload(menu.contentHolder.getBlockPos(), AddressList.clean(addresses), mode.ordinal()));
        super.removed();
    }

    @Override
    public List<Rect2i> getExtraAreas() {
        return extraAreas;
    }
}
