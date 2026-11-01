package io.github.iksanbro.smartlogistics.client;

import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * A tab with a list of addresses beside a menu: the tab opens and closes the list, "+" adds a row,
 * "x" removes one. With recipe types (smart stock ticker) every row is "recipe type → address" and
 * the type field suggests the recipe types of the game; one fixed row (mechanical crafting) comes
 * first. Without (smart frogport) every row is an address.
 *
 * <p>The panel draws and handles its own fields, so it works in Create's stock keeper menu, which
 * handles keys and clicks itself; the host screen forwards its input here first.
 */
public class AddressPanel {
    public static final int MAX_ROWS = 16;
    private static final int TAB_W = 14, TAB_H = 58;
    private static final int ROW_H = 18, VISIBLE_ROWS = 8, SUGGESTIONS = 6;
    private static final int BG = 0xF0302A26, BORDER = 0xFFB98A2E, TEXT = 0xFFF2E3B3, DIM = 0xFFA89A7A;

    private final Font font;
    private final Component title;
    private final boolean withTypes;
    @Nullable
    private final Component fixedLabel;
    @Nullable
    private final EditBox fixedBox;
    private final List<String> typeIds;
    private final Function<String, Component> typeName;
    private final List<Row> rows = new ArrayList<>();
    private boolean open;
    private boolean alwaysOpen;
    private int scroll;
    private EditBox focused;
    private boolean edited;

    // layout
    private int tabX, tabY, openTabX, closedTabX;
    private boolean leftSide;
    private int x, y, width, height;

    private final class Row {
        @Nullable
        final EditBox type;
        final EditBox address;

        Row(String typeValue, String addressValue) {
            type = withTypes ? box(typeValue, 48, "smartlogistics.gui.panel.type_hint") : null;
            address = box(addressValue, 48, "smartlogistics.gui.panel.address_hint");
        }
    }

    private AddressPanel(Font font, Component title, boolean withTypes, @Nullable Component fixedLabel, @Nullable String fixedValue,
                         List<String> typeIds, Function<String, Component> typeName) {
        this.font = font;
        this.title = title;
        this.withTypes = withTypes;
        this.fixedLabel = fixedLabel;
        this.fixedBox = fixedLabel == null ? null : box(fixedValue == null ? "" : fixedValue, 48, "smartlogistics.gui.panel.typed_address");
        this.typeIds = typeIds;
        this.typeName = typeName;
    }

    /** Addresses only (smart frogport). */
    public static AddressPanel addresses(Font font, Component title, List<String> addresses) {
        AddressPanel panel = new AddressPanel(font, title, false, null, null, List.of(), id -> Component.literal(id));
        for (String a : addresses) panel.addRow("", a);
        return panel;
    }

    /** A fixed row and "recipe type → address" rows (smart stock ticker). */
    public static AddressPanel recipeTypes(Font font, Component title, Component fixedLabel, String fixedValue,
                                           Map<String, String> typeAddresses, List<String> typeIds, Function<String, Component> typeName) {
        AddressPanel panel = new AddressPanel(font, title, true, fixedLabel, fixedValue, typeIds, typeName);
        typeAddresses.forEach(panel::addRow);
        return panel;
    }

    private EditBox box(String value, int maxLength, String hint) {
        EditBox box = new EditBox(font, 0, 0, 60, 14, Component.empty());
        box.setMaxLength(maxLength);
        box.setValue(value);
        box.setHint(Component.translatable(hint).withStyle(s -> s.withColor(0x8A7F66)));
        box.setBordered(true);
        return box;
    }

    private void addRow(String type, String address) {
        if (rows.size() < MAX_ROWS) rows.add(new Row(type, address));
    }

    public AddressPanel alwaysOpen() {
        alwaysOpen = true;
        open = true;
        return this;
    }

    /** True once the player changed anything; only then is the panel saved. */
    public boolean isEdited() {
        return edited;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open || alwaysOpen;
        tabX = this.open ? openTabX : closedTabX;
        if (!this.open) unfocus();
    }

    // values

    public List<String> addressValues() {
        List<String> out = new ArrayList<>();
        for (Row r : rows) if (!r.address.getValue().isBlank()) out.add(r.address.getValue().trim());
        return out;
    }

    public String fixedValue() {
        return fixedBox == null ? "" : fixedBox.getValue().trim();
    }

    /** recipe type → address, rows with an empty type or address left out */
    public Map<String, String> typeValues() {
        Map<String, String> out = new LinkedHashMap<>();
        for (Row r : rows) {
            if (r.type == null) continue;
            String type = normaliseType(r.type.getValue());
            String address = r.address.getValue().trim();
            if (!type.isEmpty() && !address.isEmpty()) out.put(type, address);
        }
        return out;
    }

    /** "crushing" → "create:crushing" when that is a known type; ids stay as typed. */
    private String normaliseType(String typed) {
        String t = typed.trim().toLowerCase(Locale.ROOT);
        if (t.isEmpty() || t.contains(":")) return t;
        for (String id : typeIds) if (id.endsWith(":" + t)) return id;
        for (String id : typeIds) if (typeName.apply(id).getString().equalsIgnoreCase(typed.trim())) return id;
        return t;
    }

    // layout

    /**
     * Puts the tab beside the host window: right of it if the open panel fits there, else left.
     * {@code windowLeft}/{@code windowRight} are the host window's edges, {@code top} the tab's top.
     */
    public void layout(int windowLeft, int windowRight, int top, int screenWidth, int screenHeight) {
        width = Math.min(withTypes ? 214 : 150, screenWidth - TAB_W - 4);
        int fixedH = fixedBox != null ? ROW_H + 4 : 0;
        height = 22 + fixedH + VISIBLE_ROWS * ROW_H + 22;
        y = Mth.clamp(top - 6, 2, Math.max(2, screenHeight - height - 2));
        tabY = top;
        if (alwaysOpen) {
            x = (screenWidth - width) / 2;
            y = Math.max(2, (screenHeight - height) / 2);
            return;
        }
        int roomRight = screenWidth - (windowRight + TAB_W) - 2;
        int roomLeft = windowLeft - TAB_W - 2;
        if (roomRight >= width) {
            leftSide = false;
            closedTabX = openTabX = windowRight;
            x = windowRight + TAB_W;
        } else if (roomLeft >= width) {
            leftSide = true;
            closedTabX = openTabX = windowLeft - TAB_W;
            x = openTabX - width;
        } else {
            // no room beside the window: the open panel lies over it, at the edge of the screen
            leftSide = false;
            closedTabX = Math.min(windowRight, screenWidth - TAB_W);
            x = screenWidth - width - 2;
            openTabX = x - TAB_W;
        }
        tabX = open ? openTabX : closedTabX;
    }

    private int rowsTop() {
        return y + 22 + (fixedBox != null ? ROW_H + 4 : 0);
    }

    private int typeW() {
        return (width - 20 - 14) * 11 / 20;
    }

    private void placeRow(Row r, int rowY) {
        int left = x + 6;
        int right = x + width - 6 - 14;
        if (r.type != null) {
            r.type.setX(left);
            r.type.setY(rowY);
            r.type.setWidth(typeW());
            r.address.setX(left + typeW() + 4);
            r.address.setWidth(right - (left + typeW() + 4) - 2);
        } else {
            r.address.setX(left);
            r.address.setWidth(right - left - 2);
        }
        r.address.setY(rowY);
    }

    private void clampScroll() {
        scroll = Mth.clamp(scroll, 0, Math.max(0, rows.size() - VISIBLE_ROWS));
    }

    public Rect2i area() {
        if (!open) return new Rect2i(tabX, tabY, TAB_W, TAB_H);
        int left = Math.min(x, tabX), right = Math.max(x + width, tabX + TAB_W);
        return new Rect2i(left, Math.min(y, tabY), right - left, Math.max(y + height, tabY + TAB_H) - Math.min(y, tabY));
    }

    // drawing

    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        if (!alwaysOpen) renderTab(g, mouseX, mouseY);
        if (open) renderPanel(g, mouseX, mouseY, partialTick);
        g.pose().popPose();
    }

    private void renderTab(GuiGraphics g, int mouseX, int mouseY) {
        boolean hover = mouseX >= tabX && mouseX < tabX + TAB_W && mouseY >= tabY && mouseY < tabY + TAB_H;
        g.fill(tabX, tabY, tabX + TAB_W, tabY + TAB_H, BORDER);
        g.fill(tabX + 1, tabY + 1, tabX + TAB_W - 1, tabY + TAB_H - 1, hover ? 0xFF4A3F33 : 0xFF302A26);
        g.pose().pushPose();
        g.pose().translate(tabX + 4, tabY + 5, 0);
        g.pose().mulPose(Axis.ZP.rotationDegrees(90));
        String label = (open ? "▲ " : "▼ ") + Component.translatable("smartlogistics.gui.panel.tab").getString();
        g.drawString(font, font.plainSubstrByWidth(label, TAB_H - 8), 0, -8, TEXT, false);
        g.pose().popPose();
    }

    private void renderPanel(GuiGraphics g, int mouseX, int mouseY, float pt) {
        g.fill(x, y, x + width, y + height, BORDER);
        g.fill(x + 1, y + 1, x + width - 1, y + height - 1, BG);
        g.drawString(font, title, x + 6, y + 6, TEXT, false);
        if (withTypes) {
            g.drawString(font, Component.translatable("smartlogistics.gui.panel.type"), x + 6, y + 6 + 9, DIM, false);
        }

        if (fixedBox != null) {
            int fy = y + 22;
            g.drawString(font, font.plainSubstrByWidth(fixedLabel.getString(), typeW()), x + 6, fy + 3, TEXT, false);
            fixedBox.setX(x + 6 + typeW() + 4);
            fixedBox.setY(fy);
            fixedBox.setWidth(width - 6 - 14 - (x + 6 + typeW() + 4 - x) - 2);
            fixedBox.render(g, mouseX, mouseY, pt);
        }

        clampScroll();
        int top = rowsTop();
        for (int i = 0; i < rows.size(); i++) {
            Row r = rows.get(i);
            int shown = i - scroll;
            boolean visible = shown >= 0 && shown < VISIBLE_ROWS;
            if (r.type != null) r.type.visible = visible;
            r.address.visible = visible;
            if (!visible) continue;
            int ry = top + shown * ROW_H;
            placeRow(r, ry);
            if (r.type != null) r.type.render(g, mouseX, mouseY, pt);
            r.address.render(g, mouseX, mouseY, pt);
            button(g, x + width - 6 - 12, ry + 1, "✕", mouseX, mouseY);
        }
        if (rows.isEmpty())
            g.drawString(font, Component.translatable("smartlogistics.gui.panel.empty"), x + 6, top + 4, DIM, false);
        if (rows.size() > VISIBLE_ROWS) {
            int barH = VISIBLE_ROWS * ROW_H;
            int knob = Math.max(8, barH * VISIBLE_ROWS / rows.size());
            int ky = top + (barH - knob) * scroll / Math.max(1, rows.size() - VISIBLE_ROWS);
            g.fill(x + width - 3, top, x + width - 2, top + barH, 0x40FFFFFF);
            g.fill(x + width - 3, ky, x + width - 2, ky + knob, 0xC0FFFFFF);
        }

        int addY = top + VISIBLE_ROWS * ROW_H + 4;
        if (rows.size() < MAX_ROWS) {
            button(g, x + 6, addY, "+", mouseX, mouseY);
            g.drawString(font, Component.translatable(withTypes ? "smartlogistics.gui.panel.add_type" : "smartlogistics.gui.panel.add_address"),
                    x + 22, addY + 3, DIM, false);
        }

        // suggestions under the focused type field, drawn last so they cover the rows below
        if (focused != null && isTypeBox(focused)) {
            List<String> matches = suggestions(focused.getValue());
            int sx = focused.getX(), sy = focused.getY() + 15, sw = Math.max(focused.getWidth(), 150);
            for (int i = 0; i < matches.size(); i++) {
                int ey = sy + i * 11;
                boolean hover = mouseX >= sx && mouseX < sx + sw && mouseY >= ey && mouseY < ey + 11;
                g.fill(sx, ey, sx + sw, ey + 11, hover ? 0xFF5A4A30 : 0xFF1E1A16);
                String text = typeName.apply(matches.get(i)).getString() + "  " + matches.get(i);
                g.drawString(font, font.plainSubstrByWidth(text, sw - 4), sx + 2, ey + 2, hover ? 0xFFFFFFFF : TEXT, false);
            }
        }
    }

    private void button(GuiGraphics g, int bx, int by, String text, int mouseX, int mouseY) {
        boolean hover = mouseX >= bx && mouseX < bx + 12 && mouseY >= by && mouseY < by + 12;
        g.fill(bx, by, bx + 12, by + 12, BORDER);
        g.fill(bx + 1, by + 1, bx + 11, by + 11, hover ? 0xFF6A5638 : 0xFF3A322A);
        g.drawCenteredString(font, text, bx + 6, by + 2, TEXT);
    }

    private boolean isTypeBox(EditBox box) {
        for (Row r : rows) if (r.type == box) return true;
        return false;
    }

    private List<String> suggestions(String typed) {
        String t = typed.trim().toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String id : typeIds) {
            if (out.size() >= SUGGESTIONS) break;
            if (t.isEmpty() || id.contains(t) || typeName.apply(id).getString().toLowerCase(Locale.ROOT).contains(t)) out.add(id);
        }
        return out;
    }

    // input

    private void unfocus() {
        if (focused != null) focused.setFocused(false);
        focused = null;
    }

    private void focus(EditBox box) {
        if (focused == box) return;
        unfocus();
        focused = box;
        box.setFocused(true);
    }

    private List<EditBox> visibleBoxes() {
        List<EditBox> out = new ArrayList<>();
        if (fixedBox != null) out.add(fixedBox);
        for (Row r : rows) {
            if (r.type != null && r.type.visible) out.add(r.type);
            if (r.address.visible) out.add(r.address);
        }
        return out;
    }

    private static boolean in(double mx, double my, int bx, int by, int w, int h) {
        return mx >= bx && mx < bx + w && my >= by && my < by + h;
    }

    /** True if the click was the panel's. */
    public boolean mouseClicked(double mx, double my, int button) {
        if (!alwaysOpen && in(mx, my, tabX, tabY, TAB_W, TAB_H)) {
            setOpen(!open);
            return true;
        }
        if (!open) return false;

        if (focused != null && isTypeBox(focused)) {
            List<String> matches = suggestions(focused.getValue());
            int sx = focused.getX(), sy = focused.getY() + 15, sw = Math.max(focused.getWidth(), 150);
            for (int i = 0; i < matches.size(); i++)
                if (in(mx, my, sx, sy + i * 11, sw, 11)) {
                    focused.setValue(matches.get(i));
                    edited = true;
                    unfocus();
                    return true;
                }
        }
        if (!in(mx, my, x, y, width, height)) {
            unfocus();
            return false;
        }

        int top = rowsTop();
        for (int i = 0; i < rows.size(); i++) {
            int shown = i - scroll;
            if (shown < 0 || shown >= VISIBLE_ROWS) continue;
            if (in(mx, my, x + width - 6 - 12, top + shown * ROW_H + 1, 12, 12)) {
                if (focused != null && (focused == rows.get(i).address || focused == rows.get(i).type)) unfocus();
                rows.remove(i);
                edited = true;
                clampScroll();
                return true;
            }
        }
        int addY = top + VISIBLE_ROWS * ROW_H + 4;
        if (rows.size() < MAX_ROWS && in(mx, my, x + 6, addY, 12, 12)) {
            addRow("", "");
            edited = true;
            scroll = Math.max(0, rows.size() - VISIBLE_ROWS);
            Row added = rows.get(rows.size() - 1);
            focus(added.type != null ? added.type : added.address);
            return true;
        }
        for (EditBox box : visibleBoxes()) {
            if (box.isMouseOver(mx, my)) {
                focus(box);
                box.mouseClicked(mx, my, button);
                return true;
            }
        }
        unfocus();
        return true;
    }

    public boolean mouseScrolled(double mx, double my, double scrollY) {
        if (!open || !in(mx, my, x, y, width, height)) return false;
        scroll -= (int) Math.signum(scrollY);
        clampScroll();
        return true;
    }

    public boolean isTyping() {
        return focused != null;
    }

    /** True if the key was the panel's (every key while a field is being typed in, but Escape). */
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (focused == null) return false;
        if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            if (key != GLFW.GLFW_KEY_ESCAPE && isTypeBox(focused)) {
                List<String> matches = suggestions(focused.getValue());
                if (!matches.isEmpty() && !typeIds.contains(focused.getValue().trim())) {
                    focused.setValue(matches.get(0));
                    edited = true;
                }
            }
            unfocus();
            return true;
        }
        if (key == GLFW.GLFW_KEY_TAB) {
            List<EditBox> boxes = visibleBoxes();
            int i = boxes.indexOf(focused);
            focus(boxes.get((i + 1) % boxes.size()));
            return true;
        }
        String before = focused.getValue();
        focused.keyPressed(key, scanCode, modifiers);
        edited |= !before.equals(focused.getValue());
        return true;
    }

    public boolean charTyped(char c, int modifiers) {
        if (focused == null) return false;
        focused.charTyped(c, modifiers);
        edited = true;
        return true;
    }
}
