package io.github.iksanbro.smartlogistics.mixin;

import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.packager.repackager.PackageRepackageHelper;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts.CraftingEntry;
import io.github.iksanbro.smartlogistics.Config;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The Re-Packager makes one box per craft for the mechanical crafters, holding one item per pattern
 * cell. A box has 9 slots, so Create cut every pattern after its 9th cell: in a 5 x 5 recipe most of
 * the items then left in plain boxes and were stuffed into the crafters in no order. Here a craft
 * whose pattern is bigger than 3 x 3 gets its items as full stacks, in as many boxes as needed, each
 * box carrying the pattern; the boxes of one craft leave one after the other.
 */
@Mixin(value = PackageRepackageHelper.class, remap = false)
public abstract class PackageRepackageHelperMixin {
    @Inject(method = "repackBasedOnRecipes", at = @At("HEAD"), cancellable = true)
    private void smartlogistics$bigPatterns(InventorySummary summary, PackageOrderWithCrafts order, String address, RandomSource r,
                                           CallbackInfoReturnable<List<BigItemStack>> cir) {
        if (!Config.CRAFTER_FIX.get()) return;
        boolean big = false;
        for (CraftingEntry craft : order.orderedCrafts()) big |= craft.pattern().stacks().size() > PackageItem.SLOTS;
        if (!big) return;

        List<BigItemStack> packages = new ArrayList<>();
        for (CraftingEntry craft : order.orderedCrafts()) {
            List<BigItemStack> pattern = craft.pattern().stacks();
            // as many crafts as the delivered items allow, like Create
            int crafts = 0;
            Crafts:
            for (int i = 0; i < craft.count(); i++) {
                for (BigItemStack required : pattern) {
                    if (required.stack.isEmpty()) continue;
                    if (summary.getCountOf(required.stack) <= 0) break Crafts;
                    summary.add(required.stack, -1);
                }
                crafts++;
            }
            if (crafts == 0) continue;

            InventorySummary perCraft = new InventorySummary();
            for (BigItemStack cell : pattern) if (!cell.stack.isEmpty()) perCraft.add(cell.stack, 1);
            List<ItemStack> stacks = new ArrayList<>();
            for (BigItemStack entry : perCraft.getStacks()) {
                int left = entry.count;
                while (left > 0) {
                    int n = Math.min(left, entry.stack.getMaxStackSize());
                    stacks.add(entry.stack.copyWithCount(n));
                    left -= n;
                }
            }
            List<ItemStack> boxes = new ArrayList<>();
            for (int start = 0; start < stacks.size(); start += PackageItem.SLOTS) {
                ItemStackHandler contents = new ItemStackHandler(PackageItem.SLOTS);
                for (int s = start; s < Math.min(stacks.size(), start + PackageItem.SLOTS); s++)
                    contents.setStackInSlot(s - start, stacks.get(s));
                ItemStack box = PackageItem.containing(contents);
                PackageItem.setOrder(box, r.nextInt(), 0, true, 0, true, PackageOrderWithCrafts.singleRecipe(pattern));
                boxes.add(box);
            }
            if (boxes.size() == 1) {
                packages.add(new BigItemStack(boxes.get(0), crafts));
                continue;
            }
            for (int c = 0; c < crafts; c++)
                for (ItemStack box : boxes) packages.add(new BigItemStack(box.copy(), 1));
        }
        cir.setReturnValue(packages);
    }
}
