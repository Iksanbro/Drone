package io.github.iksanbro.cbcparts.medium;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import riftyboi.cbcmodernwarfare.munitions.medium_cannon.AbstractMediumcannonProjectile;

/** Fluid round: a spout fills it (one round at a time), it splashes the fluid where it goes off. */
public class FluidMediumRoundItem extends FuzedMediumRoundItem {
    public FluidMediumRoundItem(Properties properties, MediumShell shell,
                                DeferredHolder<EntityType<?>, EntityType<?>> entity,
                                DeferredItem<MediumCartridgeItem> cartridge) {
        super(properties, shell, entity, cartridge);
    }

    /** Millibuckets the round holds: {@code fluid_shell_capacity} of its munition properties. */
    public int capacity() {
        return MediumShellKind.FLUID_PROPERTIES.getPropertiesOf(getEntityType(ItemStack.EMPTY)).fluidCapacity();
    }

    public static FluidStack fluid(ItemStack stack) {
        return stack.getOrDefault(MediumShells.FLUID_CONTENT, SimpleFluidContent.EMPTY).copy();
    }

    @Override
    public AbstractMediumcannonProjectile getMediumcannonProjectile(ItemStack stack, Level level) {
        AbstractMediumcannonProjectile projectile = super.getMediumcannonProjectile(stack, level);
        if (projectile instanceof MediumFluidProjectile fluidProjectile) fluidProjectile.setFluid(fluid(stack));
        return projectile;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        FluidStack fluid = fluid(stack);
        if (fluid.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.cbcparts.fluid_round.empty").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.cbcparts.fluid_round.fluid", fluid.getHoverName(), fluid.getAmount())
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
