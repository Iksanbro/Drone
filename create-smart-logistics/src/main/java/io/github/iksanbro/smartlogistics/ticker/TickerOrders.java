package io.github.iksanbro.smartlogistics.ticker;

import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.stockTicker.CraftableBigItemStack;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import io.github.iksanbro.smartlogistics.ticker.SmartStockTickerBlockEntity.ProcessingEntry;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;

public final class TickerOrders {
    private TickerOrders() {}

    /** The ingredients of every non-crafting recipe ordered in the stock keeper's menu, by recipe type. */
    public static List<ProcessingEntry> processingEntries(List<CraftableBigItemStack> recipes, PackageOrderWithCrafts order, Level level) {
        List<ProcessingEntry> out = new ArrayList<>();
        for (CraftableBigItemStack cbis : recipes) {
            if (cbis.recipe == null || cbis.recipe instanceof CraftingRecipe) continue;
            int perCraft = cbis.getOutputCount(level);
            if (perCraft <= 0) continue;
            int crafts = cbis.count / perCraft;
            if (crafts <= 0) continue;
            ResourceLocation type = BuiltInRegistries.RECIPE_TYPE.getKey(cbis.recipe.getType());
            if (type == null) continue;
            InventorySummary needed = new InventorySummary();
            for (Ingredient ingredient : cbis.getIngredients()) {
                if (ingredient.isEmpty()) continue;
                for (BigItemStack ordered : order.stacks()) {
                    if (ingredient.test(ordered.stack)) {
                        needed.add(ordered.stack, crafts);
                        break;
                    }
                }
            }
            if (!needed.isEmpty()) out.add(new ProcessingEntry(type.toString(), needed.getStacks()));
        }
        return out;
    }
}
