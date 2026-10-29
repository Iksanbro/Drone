package io.github.iksanbro.cbcparts;

import com.simibubi.create.AllSoundEvents;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Sneak + right click with a wrench takes a cannon part (every part this mod has a recipe for)
 * back into the inventory as the part itself, like the Create wrench does for its own blocks.
 * Breaking a part with a pickaxe still follows the part's loot table (scrap without silk touch).
 */
public final class CannonPartWrench {
    private CannonPartWrench() {}

    private static final TagKey<Item> WRENCHES = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "tools/wrench"));
    private static final ResourceLocation CREATE_WRENCH = ResourceLocation.fromNamespaceAndPath("create", "wrench");

    private static Set<Block> parts;

    /** The finished cannon parts that have a recipe of this mod, of the installed mods. */
    public static Set<Block> parts() {
        if (parts != null) return parts;
        Set<Block> found = new HashSet<>();
        ModList mods = ModList.get();
        for (GeneratedParts.UnfinishedPart part : GeneratedParts.ALL) {
            if (part.castingIngots() == 0) continue; // helper item, not a part
            if (!part.requiredMods().stream().allMatch(mods::isLoaded)) continue;
            Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(part.modId(), part.targetPath()));
            if (block != Blocks.AIR) found.add(block);
        }
        parts = Set.copyOf(found);
        return parts;
    }

    public static boolean isWrench(ItemStack stack) {
        return stack.is(WRENCHES) || BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(CREATE_WRENCH);
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (!player.isShiftKeyDown() || !isWrench(event.getItemStack())) return;
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        if (!parts().contains(state.getBlock())) return;
        if (!level.isClientSide && !Config.WRENCH_PICKUP.get()) return;

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
        if (level.isClientSide) return;
        pickUp(level, pos, state, player);
    }

    /** Removes the part and gives it to the player. Returns false if something forbade it. */
    public static boolean pickUp(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.mayInteract(player, pos) || !player.mayBuild()) return false;
        // let claim and protection mods veto it, as for breaking the block
        if (NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, state, player)).isCanceled()) return false;
        Item item = state.getBlock().asItem();
        // destroyBlock(drop = false) still runs the part's own removal: neighbours are disconnected
        // and anything loaded into it is dropped, exactly as when it is broken
        level.destroyBlock(pos, false, player);
        if (!player.isCreative() && item != Items.AIR) player.getInventory().placeItemBackInInventory(new ItemStack(item));
        AllSoundEvents.WRENCH_REMOVE.playOnServer(level, pos, 1, level.random.nextFloat() * .5f + .5f);
        return true;
    }
}
