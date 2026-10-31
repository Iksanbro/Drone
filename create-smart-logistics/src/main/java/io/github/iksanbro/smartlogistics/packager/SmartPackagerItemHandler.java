package io.github.iksanbro.smartlogistics.packager;

import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.packager.PackagerItemHandler;
import net.minecraft.world.item.ItemStack;

/**
 * Slot 0 is the box on the tray (as in Create), slots 1.. are the other finished boxes. Inserting
 * a box unpacks it; several boxes can be unpacked one after another without waiting.
 */
public class SmartPackagerItemHandler extends PackagerItemHandler {
    private final SmartPackagerBlockEntity be;

    public SmartPackagerItemHandler(SmartPackagerBlockEntity be) {
        super(be);
        this.be = be;
    }

    @Override
    public int getSlots() {
        return SmartPackagerBlockEntity.batch();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        if (slot == 0) return be.heldBox;
        int i = slot - 1;
        return i < be.readyBoxes.size() ? be.readyBoxes.get(i) : ItemStack.EMPTY;
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        if (slot == 0) {
            super.setStackInSlot(slot, stack);
            return;
        }
        int i = slot - 1;
        if (i < be.readyBoxes.size()) {
            if (stack.isEmpty()) be.readyBoxes.remove(i);
            else be.readyBoxes.set(i, stack);
            be.notifyUpdate();
        }
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (!PackageItem.isPackage(stack)) return stack;
        if (!be.unwrapBox(stack, true)) return stack;
        if (!simulate) {
            be.unwrapBox(stack, false);
            be.triggerStockCheck();
        }
        return stack.copyWithCount(stack.getCount() - 1);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (slot == 0) return super.extractItem(slot, amount, simulate);
        int i = slot - 1;
        if (amount <= 0 || i >= be.readyBoxes.size()) return ItemStack.EMPTY;
        ItemStack box = be.readyBoxes.get(i);
        if (!simulate) {
            be.readyBoxes.remove(i);
            be.notifyUpdate();
        }
        return box;
    }
}
