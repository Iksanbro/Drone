package io.github.iksanbro.smartlogistics.ticker;

import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.packager.IdentifiedInventory;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour.RequestType;
import com.simibubi.create.content.logistics.stockTicker.PackageOrder;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts.CraftingEntry;
import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;
import io.github.iksanbro.smartlogistics.registry.ModBlockEntities;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A stock ticker that knows where crafts and processing go.
 *
 * <ul>
 * <li>Crafting recipes ordered in the stock keeper's menu (crafting table and mechanical crafting)
 * go to {@link #craftingAddress}, the rest of the order to the address typed in the menu.</li>
 * <li>Processing recipes (crushing, milling, pressing, mixing... any recipe type) picked through
 * JEI or EMI go to the address set for their recipe type in {@link #typeAddresses}.</li>
 * </ul>
 * An empty address means "the address typed in the menu", as with Create's stock ticker.
 */
public class SmartStockTickerBlockEntity extends StockTickerBlockEntity {

    public String craftingAddress = "";
    /** recipe type id ("create:crushing") -> address */
    public Map<String, String> typeAddresses = new TreeMap<>();

    public SmartStockTickerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SMART_STOCK_TICKER.get(), pos, state);
    }

    public net.neoforged.neoforge.items.IItemHandler payments() {
        return receivedPayments;
    }

    public void applySettings(String crafting, Map<String, String> types) {
        craftingAddress = crafting == null ? "" : crafting.trim();
        typeAddresses = new TreeMap<>();
        types.forEach((k, v) -> {
            if (k != null && v != null && !v.isBlank()) typeAddresses.put(k, v.trim());
        });
        notifyUpdate();
    }

    public String addressForType(String recipeType) {
        return typeAddresses.getOrDefault(recipeType, "");
    }

    /** Sends the crafting part of a player's order to the crafting address. */
    @Override
    public boolean broadcastPackageRequest(RequestType type, PackageOrderWithCrafts order, @Nullable IdentifiedInventory ignoredHandler, String address) {
        if (type != RequestType.PLAYER || craftingAddress.isBlank() || order.orderedCrafts().isEmpty())
            return super.broadcastPackageRequest(type, order, ignoredHandler, address);

        InventorySummary needed = new InventorySummary();
        for (CraftingEntry craft : order.orderedCrafts())
            for (BigItemStack s : craft.pattern().stacks())
                if (!s.stack.isEmpty()) needed.add(s.stack, s.count * craft.count());

        List<BigItemStack> forCrafts = new ArrayList<>();
        List<BigItemStack> rest = new ArrayList<>();
        for (BigItemStack s : order.stacks()) {
            int toCraft = Math.min(s.count, needed.getCountOf(s.stack));
            if (toCraft > 0) {
                forCrafts.add(new BigItemStack(s.stack.copyWithCount(1), toCraft));
                needed.add(s.stack, -toCraft);
            }
            if (s.count - toCraft > 0) rest.add(new BigItemStack(s.stack.copyWithCount(1), s.count - toCraft));
        }

        boolean ok = true;
        if (!forCrafts.isEmpty())
            ok = super.broadcastPackageRequest(type, new PackageOrderWithCrafts(new PackageOrder(forCrafts), order.orderedCrafts()), ignoredHandler, craftingAddress);
        if (!rest.isEmpty())
            ok &= super.broadcastPackageRequest(type, PackageOrderWithCrafts.simple(rest), ignoredHandler, address);
        return ok;
    }

    /**
     * A player's order with processing recipes in it: the ingredients of each recipe whose type has
     * an address go there, the rest of the order as usual.
     */
    public boolean broadcastWithProcessing(PackageOrderWithCrafts order, String address, List<ProcessingEntry> processing) {
        List<BigItemStack> remaining = new ArrayList<>();
        for (BigItemStack s : order.stacks()) remaining.add(new BigItemStack(s.stack.copy(), s.count));

        boolean ok = true;
        for (ProcessingEntry entry : processing) {
            String target = addressForType(entry.recipeType());
            if (target.isBlank()) continue;
            List<BigItemStack> part = new ArrayList<>();
            for (BigItemStack want : entry.ingredients()) {
                int left = want.count;
                for (BigItemStack have : remaining) {
                    if (left <= 0) break;
                    if (have.count <= 0 || !net.minecraft.world.item.ItemStack.isSameItemSameComponents(have.stack, want.stack)) continue;
                    int take = Math.min(left, have.count);
                    have.count -= take;
                    left -= take;
                    part.add(new BigItemStack(have.stack.copyWithCount(1), take));
                }
            }
            if (!part.isEmpty())
                ok &= super.broadcastPackageRequest(RequestType.PLAYER, PackageOrderWithCrafts.simple(merge(part)), null, target);
        }

        remaining.removeIf(s -> s.count <= 0);
        if (!remaining.isEmpty())
            ok &= broadcastPackageRequest(RequestType.PLAYER,
                    new PackageOrderWithCrafts(new PackageOrder(remaining), order.orderedCrafts()), null, address);
        return ok;
    }

    private static List<BigItemStack> merge(List<BigItemStack> stacks) {
        InventorySummary summary = new InventorySummary();
        for (BigItemStack s : stacks) summary.add(s.stack, s.count);
        return summary.getStacks();
    }

    public record ProcessingEntry(String recipeType, List<BigItemStack> ingredients) {}

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putString("CraftingAddress", craftingAddress);
        CompoundTag types = new CompoundTag();
        typeAddresses.forEach(types::putString);
        tag.put("TypeAddresses", types);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        craftingAddress = tag.getString("CraftingAddress");
        typeAddresses = new TreeMap<>();
        CompoundTag types = tag.getCompound("TypeAddresses");
        for (String key : types.getAllKeys()) typeAddresses.put(key, types.getString(key));
    }
}
