package io.github.iksanbro.smartlogistics.client;

import io.github.iksanbro.smartlogistics.gauge.GridCodec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import org.lwjgl.glfw.GLFW;

/**
 * Lays out the crafting grid of a factory gauge for mechanical crafters: 3 x 3, 5 x 5, 10 x 10,
 * 25 x 25, 50 x 50 or 100 x 100 cells. The grid must match the crafters: cell (row, column) is the
 * crafter at that place, counted from the top left as seen from the front.
 *
 * <p>Pick an item from the gauge's inputs (top row) and paint cells with the left button; the right
 * button clears. Big grids scroll (wheel, shift + wheel, arrow keys). "From recipe" fills the grid
 * from a crafting or mechanical crafting recipe of the gauge's output.
 */
public class BigGridScreen extends Screen {
    private static final int CELL = 18;
    private static final ResourceLocation MECHANICAL_CRAFTING = ResourceLocation.fromNamespaceAndPath("create", "mechanical_crafting");

    private final Screen parent;
    private final List<ItemStack> palette;
    private final ItemStack output;
    private final Consumer<List<ItemStack>> onDone;

    private int side;
    private int[] cells; // 0 = empty, i = palette.get(i - 1)
    private int brush = 1;
    private int scrollX, scrollY;
    private int view;
    private int gridLeft, gridTop;
    private Button sizeButton;
    private Component message = Component.empty();

    public BigGridScreen(Screen parent, List<ItemStack> palette, List<ItemStack> current, ItemStack output, Consumer<List<ItemStack>> onDone) {
        super(Component.translatable("smartlogistics.gui.gauge.grid_title"));
        this.parent = parent;
        this.palette = palette;
        this.output = output;
        this.onDone = onDone;
        int s = GridCodec.side(current.size());
        side = s >= 3 ? s : 3;
        cells = new int[side * side];
        for (int i = 0; i < current.size() && i < cells.length; i++) cells[i] = paletteIndex(current.get(i));
        if (palette.isEmpty()) brush = 0;
    }

    private int paletteIndex(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        for (int p = 0; p < palette.size(); p++)
            if (ItemStack.isSameItemSameComponents(palette.get(p), stack)) return p + 1;
        return 0;
    }

    @Override
    protected void init() {
        view = Mth.clamp((height - 116) / CELL, 3, 14);
        view = Math.min(view, Mth.clamp((width - 40) / CELL, 3, 24));
        int shown = Math.min(view, side);
        gridLeft = width / 2 - shown * CELL / 2;
        gridTop = 62;

        int y = height - 26;
        sizeButton = addRenderableWidget(Button.builder(sizeText(), b -> cycleSize(hasShiftDown() ? -1 : 1))
                .bounds(width / 2 - 206, y, 80, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("smartlogistics.gui.gauge.from_recipe"), b -> fillFromRecipe())
                .bounds(width / 2 - 124, y, 80, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("smartlogistics.gui.gauge.fill"), b -> fillEmpty())
                .bounds(width / 2 - 42, y, 60, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("smartlogistics.gui.gauge.clear"), b -> Arrays.fill(cells, 0))
                .bounds(width / 2 + 20, y, 60, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> finish(true))
                .bounds(width / 2 + 82, y, 60, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> finish(false))
                .bounds(width / 2 + 144, y, 60, 20).build());
    }

    private Component sizeText() {
        return Component.translatable("smartlogistics.gui.gauge.size", side, side);
    }

    private void cycleSize(int dir) {
        int index = 0;
        for (int i = 0; i < GridCodec.SIZES.length; i++) if (GridCodec.SIZES[i] == side) index = i;
        int next = GridCodec.SIZES[Math.floorMod(index + dir, GridCodec.SIZES.length)];
        int[] resized = new int[next * next];
        // keep what fits, anchored at the top left
        for (int r = 0; r < Math.min(side, next); r++)
            for (int c = 0; c < Math.min(side, next); c++) resized[r * next + c] = cells[r * side + c];
        side = next;
        cells = resized;
        scrollX = scrollY = 0;
        rebuildWidgets();
    }

    private void fillEmpty() {
        if (brush == 0) return;
        for (int i = 0; i < cells.length; i++) if (cells[i] == 0) cells[i] = brush;
    }

    /** Fills the grid from a crafting or mechanical crafting recipe whose result is the gauge's output. */
    private void fillFromRecipe() {
        if (minecraft == null || minecraft.level == null || output.isEmpty()) {
            message = Component.translatable("smartlogistics.gui.gauge.no_output");
            return;
        }
        List<RecipeHolder<?>> candidates = new ArrayList<>();
        for (RecipeHolder<?> holder : minecraft.level.getRecipeManager().getRecipes()) {
            Recipe<?> recipe = holder.value();
            ResourceLocation type = BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType());
            if (recipe.getType() != RecipeType.CRAFTING && !MECHANICAL_CRAFTING.equals(type)) continue;
            if (!ItemStack.isSameItem(recipe.getResultItem(minecraft.level.registryAccess()), output)) continue;
            candidates.add(holder);
        }
        // mechanical crafting first: it is what big grids are for
        candidates.sort((a, b) -> Boolean.compare(a.value().getType() == RecipeType.CRAFTING, b.value().getType() == RecipeType.CRAFTING));
        for (RecipeHolder<?> holder : candidates) {
            Recipe<?> recipe = holder.value();
            NonNullList<Ingredient> ingredients = recipe.getIngredients();
            int w, h;
            if (recipe instanceof ShapedRecipe shaped) {
                w = shaped.getWidth();
                h = shaped.getHeight();
            } else {
                w = Math.min(side, Math.max(1, (int) Math.ceil(Math.sqrt(ingredients.size()))));
                h = (int) Math.ceil(ingredients.size() / (double) w);
            }
            if (w > side || h > side) continue;
            int[] filled = new int[side * side];
            boolean ok = true;
            for (int i = 0; i < ingredients.size() && ok; i++) {
                Ingredient ing = ingredients.get(i);
                if (ing.isEmpty()) continue;
                int p = 0;
                for (int k = 0; k < palette.size(); k++)
                    if (ing.test(palette.get(k))) {
                        p = k + 1;
                        break;
                    }
                if (p == 0) ok = false;
                filled[(i / w) * side + i % w] = p;
            }
            if (!ok) continue;
            cells = filled;
            message = Component.translatable("smartlogistics.gui.gauge.filled", holder.id().toString(), w, h);
            return;
        }
        message = Component.translatable("smartlogistics.gui.gauge.no_recipe");
    }

    private void finish(boolean save) {
        if (save) onDone.accept(GridCodec.decode(palette, cells));
        minecraft.setScreen(parent);
    }

    @Override
    public void onClose() {
        finish(false);
    }

    // painting

    private int cellAt(double mouseX, double mouseY) {
        int shown = Math.min(view, side);
        int c = (int) Math.floor((mouseX - gridLeft) / CELL);
        int r = (int) Math.floor((mouseY - gridTop) / CELL);
        if (c < 0 || r < 0 || c >= shown || r >= shown) return -1;
        return (r + scrollY) * side + (c + scrollX);
    }

    private int paletteAt(double mouseX, double mouseY) {
        int left = width / 2 - (palette.size() + 1) * 20 / 2;
        if (mouseY < 36 || mouseY >= 54) return -1;
        int i = (int) Math.floor((mouseX - left) / 20);
        return i >= 0 && i <= palette.size() ? i : -1;
    }

    private boolean paint(double mouseX, double mouseY, int button) {
        int cell = cellAt(mouseX, mouseY);
        if (cell < 0) return false;
        cells[cell] = button == 1 ? 0 : brush;
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int p = paletteAt(mouseX, mouseY);
        if (p >= 0) {
            brush = p == palette.size() ? 0 : p + 1;
            return true;
        }
        if (paint(mouseX, mouseY, button)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (paint(mouseX, mouseY, button)) return true;
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollXAmount, double scrollYAmount) {
        int step = (int) -Math.signum(scrollYAmount) * (hasControlDown() ? 5 : 1);
        if (hasShiftDown()) scrollX += step;
        else scrollY += step;
        clampScroll();
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        int step = hasControlDown() ? 5 : 1;
        switch (keyCode) {
            case GLFW.GLFW_KEY_LEFT -> scrollX -= step;
            case GLFW.GLFW_KEY_RIGHT -> scrollX += step;
            case GLFW.GLFW_KEY_UP -> scrollY -= step;
            case GLFW.GLFW_KEY_DOWN -> scrollY += step;
            default -> {
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
        }
        clampScroll();
        return true;
    }

    private void clampScroll() {
        int max = Math.max(0, side - view);
        scrollX = Mth.clamp(scrollX, 0, max);
        scrollY = Mth.clamp(scrollY, 0, max);
    }

    // drawing

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(0, 0, width, height, 0xA0101010);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 8, 0xFFFFFF);
        var hint = font.split(Component.translatable("smartlogistics.gui.gauge.hint"), width - 40);
        for (int i = 0; i < Math.min(2, hint.size()); i++)
            graphics.drawCenteredString(font, hint.get(i), width / 2, 18 + i * 9, 0xA0A0A0);

        // palette: the gauge's inputs, then the eraser
        int left = width / 2 - (palette.size() + 1) * 20 / 2;
        for (int i = 0; i <= palette.size(); i++) {
            int x = left + i * 20;
            boolean selected = i == palette.size() ? brush == 0 : brush == i + 1;
            graphics.fill(x, 36, x + 18, 54, selected ? 0xFFFFE070 : 0xFF555555);
            graphics.fill(x + 1, 37, x + 17, 53, 0xFF8B8B8B);
            if (i < palette.size()) graphics.renderItem(palette.get(i), x + 1, 37);
            else graphics.drawCenteredString(font, "✕", x + 9, 41, 0xFFFFFF);
        }

        int shown = Math.min(view, side);
        graphics.fill(gridLeft - 1, gridTop - 1, gridLeft + shown * CELL + 1, gridTop + shown * CELL + 1, 0xFF373737);
        int hovered = cellAt(mouseX, mouseY);
        for (int r = 0; r < shown; r++) {
            for (int c = 0; c < shown; c++) {
                int index = (r + scrollY) * side + (c + scrollX);
                int x = gridLeft + c * CELL, y = gridTop + r * CELL;
                graphics.fill(x + 1, y + 1, x + CELL - 1, y + CELL - 1, index == hovered ? 0xFFC6C6C6 : 0xFF8B8B8B);
                int p = cells[index];
                if (p > 0 && p <= palette.size()) graphics.renderItem(palette.get(p - 1), x + 1, y + 1);
            }
        }

        int filled = 0;
        for (int c : cells) if (c != 0) filled++;
        String info = Component.translatable("smartlogistics.gui.gauge.view", scrollY + 1, Math.min(side, scrollY + shown),
                scrollX + 1, Math.min(side, scrollX + shown), filled, cells.length).getString();
        graphics.drawCenteredString(font, info, width / 2, gridTop + shown * CELL + 4, 0xA0A0A0);
        graphics.drawCenteredString(font, message, width / 2, gridTop + shown * CELL + 15, 0xFFE070);
        sizeButton.setMessage(sizeText());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
