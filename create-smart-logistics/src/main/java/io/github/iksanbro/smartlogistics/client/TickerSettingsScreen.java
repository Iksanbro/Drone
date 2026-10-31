package io.github.iksanbro.smartlogistics.client;

import io.github.iksanbro.smartlogistics.network.TickerSettingsPayload;
import io.github.iksanbro.smartlogistics.ticker.SmartStockTickerBlockEntity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Addresses of a smart stock ticker: one for crafting recipes, one per processing recipe type.
 * Every recipe type that has recipes in the game is listed; Create's come first.
 */
public class TickerSettingsScreen extends Screen {
    private static final int ROW = 22;
    private static final int VISIBLE = 8;

    private final SmartStockTickerBlockEntity be;
    private EditBox crafting;
    private final List<Row> rows = new ArrayList<>();
    private int scroll;
    private int left, top;

    private record Row(String type, Component name, EditBox box) {}

    public TickerSettingsScreen(SmartStockTickerBlockEntity be) {
        super(Component.translatable("block.smartlogistics.smart_stock_ticker"));
        this.be = be;
    }

    @Override
    protected void init() {
        left = width / 2 - 150;
        top = Math.max(10, height / 2 - 120);
        crafting = new EditBox(font, left + 140, top + 20, 160, 16, Component.translatable("smartlogistics.gui.ticker.crafting"));
        crafting.setMaxLength(48);
        crafting.setValue(be.craftingAddress);
        crafting.setHint(Component.translatable("smartlogistics.gui.ticker.typed_address"));
        addRenderableWidget(crafting);

        rows.clear();
        for (String type : recipeTypes()) {
            EditBox box = new EditBox(font, left + 140, 0, 160, 16, Component.literal(type));
            box.setMaxLength(48);
            box.setValue(be.addressForType(type));
            box.setHint(Component.translatable("smartlogistics.gui.ticker.typed_address"));
            addRenderableWidget(box);
            rows.add(new Row(type, typeName(type), box));
        }
        layoutRows();

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(width / 2 - 50, top + 56 + VISIBLE * ROW + 6, 100, 20).build());
    }

    private List<String> recipeTypes() {
        TreeSet<String> create = new TreeSet<>();
        TreeSet<String> other = new TreeSet<>();
        if (minecraft != null && minecraft.level != null) {
            for (RecipeHolder<?> holder : minecraft.level.getRecipeManager().getRecipes()) {
                RecipeType<?> type = holder.value().getType();
                if (type == RecipeType.CRAFTING) continue;
                ResourceLocation id = BuiltInRegistries.RECIPE_TYPE.getKey(type);
                if (id == null) continue;
                (id.getNamespace().equals("create") ? create : other).add(id.toString());
            }
        }
        for (String saved : be.typeAddresses.keySet())
            if (!create.contains(saved)) (saved.startsWith("create:") ? create : other).add(saved);
        List<String> all = new ArrayList<>(create);
        all.addAll(other);
        return all;
    }

    private static Component typeName(String type) {
        return Component.translatableWithFallback("smartlogistics.recipe_type." + type.replace(':', '.'), type);
    }

    private void layoutRows() {
        scroll = Mth.clamp(scroll, 0, Math.max(0, rows.size() - VISIBLE));
        for (int i = 0; i < rows.size(); i++) {
            EditBox box = rows.get(i).box;
            int shown = i - scroll;
            box.visible = shown >= 0 && shown < VISIBLE;
            box.setY(top + 56 + shown * ROW);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll -= (int) Math.signum(scrollY);
        layoutRows();
        return true;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(left - 8, top - 4, left + 316, top + 56 + VISIBLE * ROW + 32, 0xC0101010);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, top + 4, 0xFFFFFF);
        graphics.drawString(font, Component.translatable("smartlogistics.gui.ticker.crafting"), left, top + 24, 0xFFE070);
        graphics.drawString(font, Component.translatable("smartlogistics.gui.ticker.processing"), left, top + 44, 0xA0A0A0);
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            if (!row.box.visible) continue;
            graphics.drawString(font, font.plainSubstrByWidth(row.name.getString(), 136), left, row.box.getY() + 4, 0xFFFFFF);
        }
        if (rows.size() > VISIBLE) {
            int barTop = top + 56, barH = VISIBLE * ROW - 6;
            int knobH = Math.max(10, barH * VISIBLE / rows.size());
            int knobY = barTop + (barH - knobH) * scroll / Math.max(1, rows.size() - VISIBLE);
            graphics.fill(left + 304, barTop, left + 307, barTop + barH, 0x40FFFFFF);
            graphics.fill(left + 304, knobY, left + 307, knobY + knobH, 0xC0FFFFFF);
        }
    }

    @Override
    public void onClose() {
        Map<String, String> types = new HashMap<>();
        for (Row row : rows)
            if (!row.box.getValue().isBlank()) types.put(row.type, row.box.getValue());
        PacketDistributor.sendToServer(new TickerSettingsPayload(be.getBlockPos(), crafting.getValue(), types));
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
