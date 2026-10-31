package io.github.iksanbro.smartlogistics.crafter;

import com.simibubi.create.api.packager.unpacking.UnpackingHandler;
import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlockEntity;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Puts the items of a crafting box into mechanical crafters by their place in the grid.
 *
 * <p>Create's own handler gives the n-th item of the pattern to the n-th crafter and starts crafting
 * after every box. That breaks when the recipe needs more than one box (the crafters start with half
 * a grid) and when the pattern is not exactly the size of the crafter grid (items shift to the wrong
 * crafters). Here every pattern cell (row, column) goes to the crafter at that row and column, a box
 * may bring only part of the grid, and crafting starts once every cell of the pattern is filled.
 */
public final class CrafterUnpacking {
    private CrafterUnpacking() {}

    public static boolean unpack(Level level, BlockPos pos, BlockState state, Direction side, List<ItemStack> items,
                                 PackageOrderWithCrafts orderContext, boolean simulate) {
        if (!PackageOrderWithCrafts.hasCraftingInformation(orderContext))
            return UnpackingHandler.DEFAULT.unpack(level, pos, state, side, items, null, simulate);
        if (!(level.getBlockEntity(pos) instanceof MechanicalCrafterBlockEntity crafter)) return false;
        CrafterGrid grid = CrafterGrid.of(level, crafter);
        if (grid == null) return false;

        List<BigItemStack> pattern = orderContext.getCraftingInformation();
        int width = grid.patternWidth(pattern.size());
        List<ItemStack> box = new ArrayList<>();
        for (ItemStack s : items) box.add(simulate ? s.copy() : s);

        for (int i = 0; i < pattern.size(); i++) {
            ItemStack wanted = pattern.get(i).stack;
            if (wanted.isEmpty()) continue;
            MechanicalCrafterBlockEntity target = grid.at(i / width, i % width);
            if (target == null) continue;
            var inventory = target.getInventory();
            if (!inventory.getStackInSlot(0).isEmpty()) continue;
            for (ItemStack stack : box) {
                if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, wanted)) continue;
                if (!inventory.insertItem(0, stack.copyWithCount(1), simulate).isEmpty()) break;
                stack.shrink(1);
                break;
            }
        }
        for (ItemStack stack : box)
            if (!stack.isEmpty()) return false;
        if (!simulate && isComplete(grid, pattern, width)) crafter.checkCompletedRecipe(true);
        return true;
    }

    private static boolean isComplete(CrafterGrid grid, List<BigItemStack> pattern, int width) {
        for (int i = 0; i < pattern.size(); i++) {
            ItemStack wanted = pattern.get(i).stack;
            if (wanted.isEmpty()) continue;
            MechanicalCrafterBlockEntity target = grid.at(i / width, i % width);
            if (target == null || !ItemStack.isSameItemSameComponents(target.getInventory().getStackInSlot(0), wanted)) return false;
        }
        return true;
    }
}
