package io.github.iksanbro.wpfix.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fix for a crash in Weather Physics (tornadophysics) 1.0.3 with Sable:
 * "java.lang.RuntimeException: Body has been removed" in the server tick.
 *
 * <p>{@code TornadoEvent.processPhysics} takes the rigid body of a Sable sub-level and asks it for
 * its velocity without checking that the body still exists. When a sub-level is removed during
 * the tick (split, merged, destroyed), Sable throws. The mod's own {@code WindPhysics} checks
 * {@code RigidBodyHandle.isValid()} first; this adds the same check here and skips the sub-level
 * for that tick. Does nothing when Weather Physics is not installed.
 */
@Pseudo
@Mixin(targets = "net.killey.tornadophysics.event.TornadoEvent", remap = false)
public abstract class TornadoEventMixin {
    @Inject(method = "processPhysics", at = @At("HEAD"), cancellable = true, require = 0)
    private void wpfix$skipRemovedBody(CallbackInfo ci, @Local(argsOnly = true) ServerSubLevel subLevel) {
        try {
            RigidBodyHandle handle = RigidBodyHandle.of(subLevel);
            if (handle == null || !handle.isValid()) ci.cancel();
        } catch (RuntimeException e) {
            ci.cancel();
        }
    }
}
