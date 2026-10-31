package io.github.iksanbro.smartlogistics.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import io.github.iksanbro.smartlogistics.gauge.GridCodec;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Crafting grids bigger than 3 x 3 are saved and synced in the compact form of {@link GridCodec}. */
@Mixin(value = FactoryPanelBehaviour.class, remap = false)
public abstract class FactoryPanelGridStorageMixin {
    @WrapOperation(method = "write", at = @At(value = "INVOKE",
            target = "Lnet/createmod/catnip/nbt/NBTHelper;writeItemList(Ljava/lang/Iterable;Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/nbt/ListTag;"))
    private ListTag smartlogistics$writeGrid(Iterable<ItemStack> items, HolderLookup.Provider registries, Operation<ListTag> original) {
        if (items instanceof List<ItemStack> list && GridCodec.isBig(list)) return GridCodec.toTag(list, registries);
        return original.call(items, registries);
    }

    @WrapOperation(method = "read", at = @At(value = "INVOKE",
            target = "Lnet/createmod/catnip/nbt/NBTHelper;readItemList(Lnet/minecraft/nbt/ListTag;Lnet/minecraft/core/HolderLookup$Provider;)Ljava/util/List;"))
    private List<ItemStack> smartlogistics$readGrid(ListTag tag, HolderLookup.Provider registries, Operation<List<ItemStack>> original) {
        if (GridCodec.isGridTag(tag)) return GridCodec.fromTag(tag, registries);
        return original.call(tag, registries);
    }
}
