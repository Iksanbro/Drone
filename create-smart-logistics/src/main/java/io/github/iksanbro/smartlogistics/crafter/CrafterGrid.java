package io.github.iksanbro.smartlogistics.crafter;

import com.simibubi.create.content.kinetics.crafter.CrafterHelper;
import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlock;
import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlockEntity;
import io.github.iksanbro.smartlogistics.mixin.ConnectedInputAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The crafters of one connected input group laid out as rows and columns. Row 0 is the top row,
 * column 0 the first one in the same order Create uses for its crafting patterns.
 */
public final class CrafterGrid {
    public final int width, height;
    private final MechanicalCrafterBlockEntity[][] cells;
    public final int count;

    private CrafterGrid(int width, int height, MechanicalCrafterBlockEntity[][] cells, int count) {
        this.width = width;
        this.height = height;
        this.cells = cells;
        this.count = count;
    }

    @Nullable
    public MechanicalCrafterBlockEntity at(int row, int col) {
        return row >= 0 && col >= 0 && row < height && col < width ? cells[row][col] : null;
    }

    @Nullable
    public static CrafterGrid of(Level level, MechanicalCrafterBlockEntity crafter) {
        ConnectedInputAccessor input = (ConnectedInputAccessor) crafter.getInput();
        BlockPos controllerPos = crafter.getBlockPos();
        if (!input.smartlogistics$isController()) {
            controllerPos = controllerPos.offset(input.smartlogistics$data().get(0));
            MechanicalCrafterBlockEntity controller = CrafterHelper.getCrafter(level, controllerPos);
            if (controller == null) return null;
            input = (ConnectedInputAccessor) controller.getInput();
            if (!input.smartlogistics$isController()) return null;
        }

        BlockState state = level.getBlockState(controllerPos);
        Direction facing = state.hasProperty(MechanicalCrafterBlock.HORIZONTAL_FACING)
                ? state.getValue(MechanicalCrafterBlock.HORIZONTAL_FACING) : Direction.SOUTH;
        Axis compareAxis = facing.getClockWise().getAxis();
        // the same column order as Create's ConnectedInput.getInventories
        int modifier = facing.getAxisDirection().getStep() * (compareAxis == Axis.Z ? -1 : 1);

        List<BlockPos> positions = new ArrayList<>();
        for (BlockPos offset : new ArrayList<>(input.smartlogistics$data())) positions.add(controllerPos.offset(offset));
        TreeSet<Integer> ys = new TreeSet<>((a, b) -> -Integer.compare(a, b));
        TreeSet<Integer> cs = new TreeSet<>((a, b) -> modifier * Integer.compare(a, b));
        for (BlockPos p : positions) {
            ys.add(p.getY());
            cs.add(compareAxis.choose(p.getX(), p.getY(), p.getZ()));
        }
        List<Integer> rows = new ArrayList<>(ys), cols = new ArrayList<>(cs);
        MechanicalCrafterBlockEntity[][] cells = new MechanicalCrafterBlockEntity[rows.size()][cols.size()];
        int count = 0;
        for (BlockPos p : positions) {
            MechanicalCrafterBlockEntity be = CrafterHelper.getCrafter(level, p);
            if (be == null) continue;
            cells[rows.indexOf(p.getY())][cols.indexOf(compareAxis.choose(p.getX(), p.getY(), p.getZ()))] = be;
            count++;
        }
        return new CrafterGrid(cols.size(), rows.size(), cells, count);
    }

    /**
     * Width of a crafting pattern of {@code size} cells for this grid: the grid's own width when the
     * pattern fills it or is made of whole rows of it, 3 for a 3 x 3 crafting table pattern, the side of
     * a square pattern (big factory gauge grids).
     */
    public int patternWidth(int size) {
        if (size == width * height) return width;
        if (size == 9) return 3;
        int side = (int) Math.round(Math.sqrt(size));
        if (side * side == size) return side;
        if (width > 0 && size % width == 0) return width;
        return width;
    }
}
