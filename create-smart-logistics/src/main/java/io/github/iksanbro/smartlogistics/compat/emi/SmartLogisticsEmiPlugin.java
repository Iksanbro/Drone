package io.github.iksanbro.smartlogistics.compat.emi;

import com.simibubi.create.AllMenuTypes;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.stockTicker.CraftableBigItemStack;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestMenu;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.recipe.handler.EmiRecipeHandler;
import dev.emi.emi.api.stack.EmiStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * EMI's "+" button in the stock keeper's menu: like Create's JEI support, it orders the ingredients of
 * the recipe from the network stock. Works with any recipe type (crafting, crushing, mixing...); a
 * smart stock ticker then sends them to the address of that recipe type.
 */
@EmiEntrypoint
public class SmartLogisticsEmiPlugin implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        registry.addRecipeHandler(AllMenuTypes.STOCK_KEEPER_REQUEST.get(), new StockKeeperHandler());
    }

    static class StockKeeperHandler implements EmiRecipeHandler<StockKeeperRequestMenu> {
        @Override
        public EmiPlayerInventory getInventory(AbstractContainerScreen<StockKeeperRequestMenu> screen) {
            List<EmiStack> stacks = new ArrayList<>();
            InventorySummary stock = screen.getMenu().contentHolder.getLastClientsideStockSnapshotAsSummary();
            if (stock != null)
                for (BigItemStack s : stock.getStacks()) stacks.add(EmiStack.of(s.stack, Math.max(0, s.count)));
            return new EmiPlayerInventory(stacks);
        }

        @Override
        public boolean supportsRecipe(EmiRecipe recipe) {
            RecipeHolder<?> holder = recipe.getBackingRecipe();
            return holder != null && holder.value().getIngredients().size() <= 81;
        }

        @Override
        public boolean canCraft(EmiRecipe recipe, EmiCraftContext<StockKeeperRequestMenu> context) {
            return context.getScreen() instanceof StockKeeperRequestScreen && context.getInventory().canCraft(recipe);
        }

        @Override
        public boolean craft(EmiRecipe recipe, EmiCraftContext<StockKeeperRequestMenu> context) {
            if (!(context.getScreen() instanceof StockKeeperRequestScreen screen)) return false;
            RecipeHolder<?> holder = recipe.getBackingRecipe();
            if (holder == null) return false;
            Recipe<?> r = holder.value();
            for (CraftableBigItemStack ordered : screen.recipesToOrder)
                if (ordered.recipe == r) return false;
            if (screen.itemsToOrder.size() >= 9) return false;
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return false;
            ItemStack result = r.getResultItem(mc.level.registryAccess());
            if (result.isEmpty()) return false;
            CraftableBigItemStack cbis = new CraftableBigItemStack(result, r);
            screen.recipesToOrder.add(cbis);
            screen.searchBox.setValue("");
            screen.refreshSearchNextTick = true;
            screen.requestCraftable(cbis, Math.max(1, context.getAmount()) * result.getCount());
            mc.setScreen(screen);
            return true;
        }
    }
}
