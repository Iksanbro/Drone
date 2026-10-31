package io.github.iksanbro.smartlogistics.packager;

import com.simibubi.create.api.packager.unpacking.UnpackingHandler;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.packager.PackagerBlock;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.packager.PackagerItemHandler;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.foundation.item.ItemHelper;
import io.github.iksanbro.smartlogistics.Config;
import io.github.iksanbro.smartlogistics.registry.ModBlockEntities;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * A packager with its address and an auto-packaging switch in its menu, that packs and unpacks
 * {@link Config#PACKAGER_BATCH} boxes at a time.
 *
 * <p>Create's packager holds one finished box ({@link #heldBox}) and queues the rest, letting one out
 * every 20 ticks. This one also keeps up to batch - 1 more finished boxes ready to be taken, and
 * accepts up to batch boxes to unpack in those 20 ticks.
 */
public class SmartPackagerBlockEntity extends PackagerBlockEntity {

    public String customAddress = "";
    public boolean autoPack;

    /** Finished boxes besides {@link #heldBox}, ready to be taken. */
    public final List<ItemStack> readyBoxes = new ArrayList<>();
    private int unpackBudget;
    private int budgetTimer;

    public final SmartPackagerItemHandler smartInventory = new SmartPackagerItemHandler(this);

    public SmartPackagerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SMART_PACKAGER.get(), pos, state);
    }

    public static int batch() {
        return Config.PACKAGER_BATCH.get();
    }

    public void applySettings(String address, boolean auto) {
        customAddress = address == null ? "" : address.trim();
        autoPack = auto;
        updateSignAddress();
        notifyUpdate();
    }

    @Override
    public void updateSignAddress() {
        super.updateSignAddress();
        // the address from the menu wins over signs, a computer's address wins over both
        if (!customAddress.isBlank() && !(computerBehaviour.hasAttachedComputer() && hasCustomComputerAddress))
            signBasedAddress = customAddress;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide()) return;
        if (--budgetTimer <= 0) {
            budgetTimer = CYCLE;
            unpackBudget = batch();
        }
        // finished boxes waiting in Create's queue become ready right away, up to the batch
        boolean moved = false;
        while (!queuedExitingPackages.isEmpty() && readyBoxes.size() < batch() - 1) {
            var entry = queuedExitingPackages.get(0);
            readyBoxes.add(entry.stack.copy());
            if (--entry.count <= 0) queuedExitingPackages.remove(0);
            moved = true;
        }
        if (moved) {
            notifyUpdate();
            wakeTheFrogs();
        }
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (level == null || level.isClientSide() || !autoPack || !redstoneModeActive()) return;
        updateSignAddress();
        attemptToSend(null);
        packMore();
    }

    @Override
    public void activate() {
        super.activate();
        if (redstoneModeActive()) packMore();
    }

    /** Packs the rest of the batch next to the box Create's code just made. */
    public void packMore() {
        int made = 0;
        while (readyBoxes.size() < batch() - 1 && made < batch() - 1) {
            ItemStack box = packOneBox();
            if (box.isEmpty()) break;
            readyBoxes.add(box);
            made++;
        }
        if (made > 0) {
            notifyUpdate();
            wakeTheFrogs();
        }
    }

    /** One box from the attached inventory, as Create's packager makes it when powered. */
    private ItemStack packOneBox() {
        IItemHandler targetInv = targetInventory.getInventory();
        if (targetInv == null || targetInv instanceof PackagerItemHandler) return ItemStack.EMPTY;
        ItemStackHandler extracted = new ItemStackHandler(PackageItem.SLOTS);
        boolean any = false;
        Outer:
        for (int i = 0; i < PackageItem.SLOTS; i++) {
            for (int slot = 0; slot < targetInv.getSlots(); slot++) {
                ItemStack stack = targetInv.extractItem(slot, 64, true);
                if (stack.isEmpty()) continue;
                // a box already in the inventory is sent on as it is
                if (stack.getItem() instanceof PackageItem) {
                    if (any) continue;
                    ItemStack box = targetInv.extractItem(slot, 1, false);
                    PackageItem.clearAddress(box);
                    if (!signBasedAddress.isBlank()) PackageItem.addAddress(box, signBasedAddress);
                    return box;
                }
                boolean bulky = !stack.getItem().canFitInsideContainerItems();
                if (bulky && any) continue;
                int leftover = ItemHandlerHelper.insertItemStacked(extracted, stack.copy(), false).getCount();
                int moved = stack.getCount() - leftover;
                if (moved <= 0) continue;
                targetInv.extractItem(slot, moved, false);
                any = true;
                if (bulky) break Outer;
            }
        }
        if (!any) return ItemStack.EMPTY;
        ItemStack box = PackageItem.containing(extracted);
        PackageItem.clearAddress(box);
        if (!signBasedAddress.isBlank()) PackageItem.addAddress(box, signBasedAddress);
        triggerStockCheck();
        return box;
    }

    /** Unpacks while the batch for these 20 ticks lasts, not only between animations. */
    @Override
    public boolean unwrapBox(ItemStack box, boolean simulate) {
        if (level == null || unpackBudget <= 0) return false;
        List<ItemStack> items = ItemHelper.getNonEmptyStacks(PackageItem.getContents(box));
        if (items.isEmpty()) return true;
        PackageOrderWithCrafts orderContext = PackageItem.getOrderContext(box);
        Direction facing = getBlockState().getOptionalValue(PackagerBlock.FACING).orElse(Direction.UP);
        BlockPos target = worldPosition.relative(facing.getOpposite());
        BlockState targetState = level.getBlockState(target);
        UnpackingHandler handler = UnpackingHandler.REGISTRY.get(targetState);
        UnpackingHandler toUse = handler != null ? handler : UnpackingHandler.DEFAULT;
        boolean unpacked = toUse.unpack(level, target, targetState, facing, items, orderContext, simulate);
        if (unpacked && !simulate) {
            unpackBudget--;
            previouslyUnwrapped = box;
            if (animationTicks == 0) {
                animationInward = true;
                animationTicks = CYCLE;
            }
            notifyUpdate();
        }
        return unpacked;
    }

    @Override
    public void destroy() {
        super.destroy();
        for (ItemStack box : readyBoxes)
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), box);
        readyBoxes.clear();
    }

    @Override
    public void clearContent() {
        super.clearContent();
        readyBoxes.clear();
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putString("CustomAddress", customAddress);
        tag.putBoolean("AutoPack", autoPack);
        ListTag boxes = new ListTag();
        for (ItemStack box : readyBoxes) boxes.add(box.saveOptional(registries));
        tag.put("ReadyBoxes", boxes);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        customAddress = tag.getString("CustomAddress");
        autoPack = tag.getBoolean("AutoPack");
        readyBoxes.clear();
        for (Tag t : tag.getList("ReadyBoxes", Tag.TAG_COMPOUND)) {
            ItemStack box = ItemStack.parseOptional(registries, (CompoundTag) t);
            if (!box.isEmpty()) readyBoxes.add(box);
        }
    }

    /** All finished boxes: the one on the tray and the ready ones. */
    public int finishedBoxes() {
        return (heldBox.isEmpty() ? 0 : 1) + readyBoxes.size();
    }
}
