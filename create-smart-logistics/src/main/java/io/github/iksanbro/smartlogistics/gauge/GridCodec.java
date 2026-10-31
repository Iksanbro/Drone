package io.github.iksanbro.smartlogistics.gauge;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

/**
 * Crafting grids of factory gauges as a palette of items plus one number per cell. A 100 x 100 grid
 * written item by item takes hundreds of kilobytes in the gauge's data and does not fit in a packet
 * from the client at all; this way it takes about 10 kB.
 */
public final class GridCodec {
    private GridCodec() {}

    public static final int MAX_CELLS = 100 * 100;
    public static final int[] SIZES = {3, 5, 10, 25, 50, 100};

    public record Encoded(List<ItemStack> palette, int[] cells) {}

    /** Side of the square grid a list of cells makes, or 0. */
    public static int side(int cells) {
        int n = (int) Math.round(Math.sqrt(cells));
        return n * n == cells ? n : 0;
    }

    public static boolean isBig(List<ItemStack> arrangement) {
        return arrangement.size() > 9;
    }

    public static Encoded encode(List<ItemStack> arrangement) {
        List<ItemStack> palette = new ArrayList<>();
        int[] cells = new int[arrangement.size()];
        for (int i = 0; i < cells.length; i++) {
            ItemStack stack = arrangement.get(i);
            if (stack.isEmpty()) continue;
            int index = -1;
            for (int p = 0; p < palette.size(); p++)
                if (ItemStack.isSameItemSameComponents(palette.get(p), stack)) {
                    index = p;
                    break;
                }
            if (index < 0) {
                palette.add(stack.copyWithCount(1));
                index = palette.size() - 1;
            }
            cells[i] = index + 1;
        }
        return new Encoded(palette, cells);
    }

    public static List<ItemStack> decode(List<ItemStack> palette, int[] cells) {
        List<ItemStack> out = new ArrayList<>(Math.min(cells.length, MAX_CELLS));
        for (int i = 0; i < cells.length && i < MAX_CELLS; i++) {
            int c = cells[i];
            out.add(c > 0 && c <= palette.size() ? palette.get(c - 1).copyWithCount(1) : ItemStack.EMPTY);
        }
        return out;
    }

    // NBT: a list holding one compound, so it fits where Create keeps its item list

    public static ListTag toTag(List<ItemStack> arrangement, HolderLookup.Provider registries) {
        Encoded e = encode(arrangement);
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("SmartGrid", true);
        ListTag palette = new ListTag();
        for (ItemStack s : e.palette()) palette.add(s.saveOptional(registries));
        tag.put("Palette", palette);
        tag.putIntArray("Cells", e.cells());
        ListTag list = new ListTag();
        list.add(tag);
        return list;
    }

    public static boolean isGridTag(ListTag list) {
        return list.size() == 1 && list.get(0) instanceof CompoundTag c && c.getBoolean("SmartGrid");
    }

    public static List<ItemStack> fromTag(ListTag list, HolderLookup.Provider registries) {
        CompoundTag tag = list.getCompound(0);
        List<ItemStack> palette = new ArrayList<>();
        for (Tag t : tag.getList("Palette", Tag.TAG_COMPOUND)) palette.add(ItemStack.parseOptional(registries, (CompoundTag) t));
        return decode(palette, tag.getIntArray("Cells"));
    }
}
