package io.github.iksanbro.smartlogistics.frogport;

import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.packagePort.PackagePortMenu;
import com.simibubi.create.content.logistics.packagePort.frogport.FrogportBlockEntity;
import com.simibubi.create.content.logistics.packager.PackagerItemHandler;
import io.github.iksanbro.smartlogistics.Config;
import io.github.iksanbro.smartlogistics.registry.ModBlockEntities;
import io.github.iksanbro.smartlogistics.registry.ModMenus;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * A frogport with several addresses, a send / receive mode, and more packages at a time.
 *
 * <p>The frog still animates one package, as Create's frogport does. Next to it, up to
 * {@link Config#FROGPORT_BATCH} - 1 more packages are caught from the chain at the same time (they
 * land after the same delay), and as many more are sent onto the chain each time the frog sends one.
 */
public class SmartFrogportBlockEntity extends FrogportBlockEntity {

    public enum Mode {
        /** sends packages that are not for it and catches the ones that are */
        BOTH,
        /** only sends: catches nothing, sends every package */
        SEND_ONLY,
        /** only catches: never sends anything */
        RECEIVE_ONLY;

        public static Mode byId(int id) {
            return id >= 0 && id < values().length ? values()[id] : BOTH;
        }
    }

    /** Ticks a caught package flies, the same as the frog's animation (chaser 0 -> 1 at 0.1 a tick). */
    private static final int CATCH_TICKS = 10;

    public List<String> addresses = new ArrayList<>();
    public Mode mode = Mode.BOTH;

    private final List<Incoming> incoming = new ArrayList<>();

    private static final class Incoming {
        final ItemStack box;
        int ticksLeft;

        Incoming(ItemStack box, int ticksLeft) {
            this.box = box;
            this.ticksLeft = ticksLeft;
        }
    }

    public SmartFrogportBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SMART_FROGPORT.get(), pos, state);
        setLazyTickRate(5);
    }

    public IItemHandler automationHandler() {
        return itemHandler;
    }

    public static int batch() {
        return Config.FROGPORT_BATCH.get();
    }

    @Override
    public String getFilterString() {
        if (mode == Mode.SEND_ONLY) return null;
        return AddressList.encode(addresses.isEmpty() ? List.of(addressFilter) : addresses);
    }

    public void applySettings(List<String> newAddresses, Mode newMode) {
        addresses = AddressList.clean(newAddresses);
        addressFilter = addresses.isEmpty() ? "" : addresses.get(0);
        mode = newMode;
        acceptsPackages = newMode != Mode.SEND_ONLY;
        filterChanged();
        notifyUpdate();
    }

    // catching

    private int freeSlots() {
        int free = 0;
        for (int i = 0; i < inventory.getSlots(); i++)
            if (inventory.getStackInSlot(i).isEmpty()) free++;
        free -= incoming.size();
        if (isAnimationInProgress() && !currentlyDepositing) free--;
        return free;
    }

    /** A chain conveyor offers a package addressed to this port. */
    public boolean tryCatch(ItemStack box) {
        if (mode == Mode.SEND_ONLY || level == null || freeSlots() <= 0) return false;
        if (!isAnimationInProgress()) {
            startAnimation(box, false);
            return true;
        }
        if (incoming.size() + 1 >= batch()) return false;
        incoming.add(new Incoming(box.copy(), CATCH_TICKS));
        setChanged();
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide() || incoming.isEmpty()) return;
        boolean changed = false;
        for (Iterator<Incoming> it = incoming.iterator(); it.hasNext(); ) {
            Incoming in = it.next();
            if (--in.ticksLeft > 0) continue;
            ItemStack rest = ItemHandlerHelper.insertItem(inventory, in.box.copy(), false);
            if (!rest.isEmpty()) drop(rest);
            it.remove();
            changed = true;
        }
        if (changed) setChanged();
    }

    // sending

    @Override
    public void tryPullingFromOwnAndAdjacentInventories() {
        if (mode == Mode.RECEIVE_ONLY) return;
        boolean wasAnimating = isAnimationInProgress();
        super.tryPullingFromOwnAndAdjacentInventories();
        if (wasAnimating || !isAnimationInProgress() || !currentlyDepositing || target == null) return;
        // the frog took one; the rest of the batch goes straight onto the chain
        for (int sent = 1; sent < batch(); sent++)
            if (!sendOneMore()) break;
    }

    private boolean sendOneMore() {
        if (sendFrom(inventory)) return true;
        IItemHandler below = getAdjacentInventory(Direction.DOWN);
        return below != null && sendFrom(below);
    }

    private boolean sendFrom(IItemHandler handler) {
        String filter = getFilterString();
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.extractItem(slot, 1, true);
            if (stack.isEmpty() || !PackageItem.isPackage(stack)) continue;
            if (filter != null && !(handler instanceof PackagerItemHandler) && PackageItem.matchAddress(stack, filter)) continue;
            if (!target.export(level, worldPosition, stack.copy(), true)) return false;
            ItemStack taken = handler.extractItem(slot, 1, false);
            if (taken.isEmpty()) continue;
            if (!target.export(level, worldPosition, taken, false)) {
                ItemStack rest = ItemHandlerHelper.insertItem(handler, taken, false);
                if (!rest.isEmpty()) drop(rest);
                return false;
            }
            level.blockEntityChanged(worldPosition);
            return true;
        }
        return false;
    }

    // menu

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new PackagePortMenu(ModMenus.SMART_FROGPORT.get(), id, playerInventory, this);
    }

    @Override
    public void destroy() {
        super.destroy();
        for (Incoming in : incoming) drop(in.box);
        incoming.clear();
    }

    // saving

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        ListTag list = new ListTag();
        for (String a : addresses) list.add(StringTag.valueOf(a));
        tag.put("SmartAddresses", list);
        tag.putInt("SmartMode", mode.ordinal());
        if (clientPacket) return;
        ListTag flying = new ListTag();
        for (Incoming in : incoming) {
            CompoundTag t = new CompoundTag();
            t.put("Box", in.box.saveOptional(registries));
            t.putInt("Ticks", in.ticksLeft);
            flying.add(t);
        }
        tag.put("SmartIncoming", flying);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        addresses = new ArrayList<>();
        for (Tag t : tag.getList("SmartAddresses", Tag.TAG_STRING)) addresses.add(t.getAsString());
        mode = Mode.byId(tag.getInt("SmartMode"));
        if (clientPacket) return;
        incoming.clear();
        for (Tag t : tag.getList("SmartIncoming", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            ItemStack box = ItemStack.parseOptional(registries, c.getCompound("Box"));
            if (!box.isEmpty()) incoming.add(new Incoming(box, c.getInt("Ticks")));
        }
    }
}
