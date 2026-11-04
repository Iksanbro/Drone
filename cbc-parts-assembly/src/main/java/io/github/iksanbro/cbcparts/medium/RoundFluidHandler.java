package io.github.iksanbro.cbcparts.medium;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

/**
 * Fluid tank of a medium fluid round, kept in the {@link MediumShells#FLUID_CONTENT} component.
 *
 * <p>Unlike NeoForge's {@code FluidHandlerItemStack} it answers for one round even when the stack
 * holds several: Create's spout asks the whole stack how much it takes, then fills a single round
 * split off the stack.
 */
public final class RoundFluidHandler implements IFluidHandlerItem {
    private final ItemStack stack;
    private final int capacity;

    public RoundFluidHandler(ItemStack stack, int capacity) {
        this.stack = stack;
        this.capacity = capacity;
    }

    private FluidStack contents() {
        return FluidMediumRoundItem.fluid(stack);
    }

    private void set(FluidStack fluid) {
        if (fluid.isEmpty()) stack.remove(MediumShells.FLUID_CONTENT);
        else stack.set(MediumShells.FLUID_CONTENT, SimpleFluidContent.copyOf(fluid));
    }

    @Override
    public ItemStack getContainer() {
        return stack;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return contents();
    }

    @Override
    public int getTankCapacity(int tank) {
        return capacity;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack fluid) {
        return true;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) return 0;
        FluidStack current = contents();
        if (!current.isEmpty() && !FluidStack.isSameFluidSameComponents(current, resource)) return 0;
        int amount = Math.min(capacity - current.getAmount(), resource.getAmount());
        if (amount <= 0) return 0;
        if (action.execute()) set(resource.copyWithAmount(current.getAmount() + amount));
        return amount;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        FluidStack current = contents();
        if (resource.isEmpty() || !FluidStack.isSameFluidSameComponents(current, resource)) return FluidStack.EMPTY;
        return drain(resource.getAmount(), action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        FluidStack current = contents();
        int amount = Math.min(maxDrain, current.getAmount());
        if (amount <= 0) return FluidStack.EMPTY;
        if (action.execute()) set(current.copyWithAmount(current.getAmount() - amount));
        return current.copyWithAmount(amount);
    }
}
