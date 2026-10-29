package io.github.iksanbro.cbcparts;

import com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Transitional item of a sequenced assembly recipe. Its name is taken from the finished part. */
public class UnfinishedPartItem extends SequencedAssemblyItem {
    private final ResourceLocation target;

    public UnfinishedPartItem(Item.Properties properties, ResourceLocation target) {
        super(properties);
        this.target = target;
    }

    public ResourceLocation getTarget() {
        return target;
    }

    @Override
    public Component getName(ItemStack stack) {
        Item finished = BuiltInRegistries.ITEM.get(target);
        Component finishedName = finished == Items.AIR
                ? Component.literal(target.toString())
                : Component.translatable(finished.getDescriptionId());
        return Component.translatable("item.cbcparts.unfinished", finishedName);
    }
}
