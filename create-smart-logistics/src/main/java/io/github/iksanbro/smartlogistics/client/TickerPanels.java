package io.github.iksanbro.smartlogistics.client;

import io.github.iksanbro.smartlogistics.network.TickerSettingsPayload;
import io.github.iksanbro.smartlogistics.ticker.SmartStockTickerBlockEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.network.PacketDistributor;

/** The address panel of a smart stock ticker, and saving it. */
public final class TickerPanels {
    private TickerPanels() {}

    public static AddressPanel create(SmartStockTickerBlockEntity be, Font font) {
        return AddressPanel.recipeTypes(font, Component.translatable("smartlogistics.gui.ticker.title"),
                Component.translatable("smartlogistics.gui.ticker.crafting"), be.craftingAddress, be.typeAddresses,
                recipeTypes(), TickerPanels::typeName);
    }

    /** Sends the panel's addresses if they differ from the ticker's. */
    public static void save(SmartStockTickerBlockEntity be, AddressPanel panel) {
        if (!panel.isEdited()) return;
        String crafting = panel.fixedValue();
        Map<String, String> types = panel.typeValues();
        if (Objects.equals(crafting, be.craftingAddress) && types.equals(be.typeAddresses)) return;
        be.craftingAddress = crafting;
        be.typeAddresses = new java.util.TreeMap<>(types);
        PacketDistributor.sendToServer(new TickerSettingsPayload(be.getBlockPos(), crafting, new java.util.HashMap<>(types)));
    }

    /** Recipe types that have recipes, Create's first; crafting is the fixed row. */
    public static List<String> recipeTypes() {
        TreeSet<String> create = new TreeSet<>(), other = new TreeSet<>();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null)
            for (RecipeHolder<?> holder : mc.level.getRecipeManager().getRecipes()) {
                RecipeType<?> type = holder.value().getType();
                if (type == RecipeType.CRAFTING) continue;
                ResourceLocation id = BuiltInRegistries.RECIPE_TYPE.getKey(type);
                if (id == null || id.toString().equals("create:mechanical_crafting")) continue;
                (id.getNamespace().equals("create") ? create : other).add(id.toString());
            }
        List<String> all = new ArrayList<>(create);
        all.addAll(other);
        return all;
    }

    public static Component typeName(String type) {
        return Component.translatableWithFallback("smartlogistics.recipe_type." + type.replace(':', '.'), type);
    }
}
