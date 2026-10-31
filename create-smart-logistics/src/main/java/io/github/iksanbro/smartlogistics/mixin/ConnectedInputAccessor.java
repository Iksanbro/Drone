package io.github.iksanbro.smartlogistics.mixin;

import com.simibubi.create.content.kinetics.crafter.ConnectedInputHandler.ConnectedInput;
import java.util.List;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = ConnectedInput.class, remap = false)
public interface ConnectedInputAccessor {
    @Accessor("data")
    List<BlockPos> smartlogistics$data();

    @Accessor("isController")
    boolean smartlogistics$isController();
}
