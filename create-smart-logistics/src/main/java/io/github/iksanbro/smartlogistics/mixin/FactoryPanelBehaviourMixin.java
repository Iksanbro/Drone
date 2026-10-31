package io.github.iksanbro.smartlogistics.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import io.github.iksanbro.smartlogistics.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Factory gauges and unloaded chunks.
 *
 * <p>A gauge counts the stock of its whole logistics network. When any stock link, packager link
 * or other part of the network sits in an unloaded chunk, Create cannot count it, so every gauge
 * of that network shows "?" and stops ordering until the chunk loads. With the fix the gauge
 * counts what is loaded and keeps working.
 */
@Mixin(value = FactoryPanelBehaviour.class, remap = false)
public abstract class FactoryPanelBehaviourMixin {
    @WrapOperation(method = "tickStorageMonitor", at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelBehaviour;getUnloadedLinks()I"))
    private int smartlogistics$ignoreUnloaded(FactoryPanelBehaviour panel, Operation<Integer> original) {
        int unloaded = original.call(panel);
        return Config.GAUGES_IGNORE_UNLOADED.get() ? 0 : unloaded;
    }
}
