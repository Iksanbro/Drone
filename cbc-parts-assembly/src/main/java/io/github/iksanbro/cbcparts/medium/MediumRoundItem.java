package io.github.iksanbro.cbcparts.medium;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import rbasamoyai.createbigcannons.munitions.fuzes.FuzeItem;
import riftyboi.cbcmodernwarfare.munitions.ItemStackDataUtil;
import riftyboi.cbcmodernwarfare.munitions.medium_cannon.AbstractMediumcannonProjectile;
import riftyboi.cbcmodernwarfare.munitions.medium_cannon.MediumcannonFuzedProjectile;
import riftyboi.cbcmodernwarfare.munitions.medium_cannon.MediumcannonRoundItem;
import riftyboi.cbcmodernwarfare.munitions.medium_cannon.config.MediumcannonProjectilePropertiesComponent;

/** A medium cannon round. Put together with a filled cartridge it becomes a cartridge the cannon fires. */
public class MediumRoundItem extends MediumcannonRoundItem {
    protected final MediumShell shell;
    private final DeferredHolder<EntityType<?>, EntityType<?>> entity;
    private final DeferredItem<MediumCartridgeItem> cartridge;

    public MediumRoundItem(Properties properties, MediumShell shell,
                           DeferredHolder<EntityType<?>, EntityType<?>> entity,
                           DeferredItem<MediumCartridgeItem> cartridge) {
        super(properties);
        this.shell = shell;
        this.entity = entity;
        this.cartridge = cartridge;
    }

    public MediumShell shell() {
        return shell;
    }

    @Override
    public AbstractMediumcannonProjectile getMediumcannonProjectile(ItemStack stack, Level level) {
        if (!(entity.get().create(level) instanceof AbstractMediumcannonProjectile projectile)) return null;
        if (projectile instanceof MediumcannonFuzedProjectile fuzed) {
            ItemStack fuze = ItemStackDataUtil.getFuze(stack);
            if (!fuze.isEmpty()) fuzed.setFuze(fuze);
        }
        return projectile;
    }

    @Nonnull
    @Override
    public MediumcannonProjectilePropertiesComponent getMediumcannonProperties(ItemStack stack) {
        return shell.kind().mediumcannonProperties(entity.get());
    }

    @Override
    public EntityType<?> getEntityType(ItemStack stack) {
        return entity.get();
    }

    @Override
    public ItemStack getCartridgeType() {
        return new ItemStack(cartridge.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        ItemStack fuze = ItemStackDataUtil.getFuze(stack);
        if (fuze.isEmpty()) return;
        tooltip.add(Component.translatable("block.createbigcannons.shell.tooltip.fuze")
                .append(" ").append(fuze.getHoverName()).withStyle(ChatFormatting.GRAY));
        if (fuze.getItem() instanceof FuzeItem) {
            List<Component> sub = new ArrayList<>();
            fuze.getItem().appendHoverText(fuze, context, sub, flag);
            for (Component line : sub) tooltip.add(Component.literal("  ").append(line).withStyle(ChatFormatting.GRAY));
        }
    }
}
