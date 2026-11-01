package io.github.iksanbro.smartlogistics.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.compat.jei.StockKeeperTransferHandler;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * JEI's "+" in the stock keeper's menu for big recipes. Create refused recipes with more than 9
 * ingredients and orders with 9 kinds of items already, and matched the recipe against 9 slots.
 * Only loaded with JEI.
 */
@Mixin(value = StockKeeperTransferHandler.class, remap = false)
public abstract class StockKeeperTransferHandlerMixin {
    @ModifyConstant(method = "transferRecipeOnClient", constant = @Constant(intValue = 9, ordinal = 0))
    private int smartlogistics$anyIngredientCount(int nine) {
        return 100 * 100;
    }

    @ModifyConstant(method = "transferRecipeOnClient", constant = @Constant(intValue = 9, ordinal = 1))
    private int smartlogistics$anyOrderSize(int nine) {
        return Integer.MAX_VALUE;
    }

    @ModifyConstant(method = "transferRecipeOnClient", constant = @Constant(intValue = 9, ordinal = 2))
    private int smartlogistics$slotPerIngredient(int nine, @Local(argsOnly = true) IRecipeSlotsView recipeSlots) {
        return Math.max(nine, recipeSlots.getSlotViews(RecipeIngredientRole.INPUT).size());
    }
}
