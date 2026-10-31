package io.github.iksanbro.smartlogistics.test;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorPackage;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.packagePort.PackagePortTarget.ChainConveyorFrogportTarget;
import com.simibubi.create.content.logistics.packagePort.frogport.FrogportBlockEntity;
import com.simibubi.create.content.logistics.packager.PackagerBlock;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour.RequestType;
import com.simibubi.create.content.logistics.packagerLink.PackagerLinkBlockEntity;
import com.simibubi.create.content.logistics.stockTicker.PackageOrder;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts.CraftingEntry;
import io.github.iksanbro.smartlogistics.Config;
import io.github.iksanbro.smartlogistics.SmartLogistics;
import io.github.iksanbro.smartlogistics.frogport.AddressList;
import io.github.iksanbro.smartlogistics.frogport.SmartFrogportBlockEntity;
import io.github.iksanbro.smartlogistics.gauge.GridCodec;
import io.github.iksanbro.smartlogistics.packager.SmartPackagerBlockEntity;
import io.github.iksanbro.smartlogistics.registry.ModBlocks;
import io.github.iksanbro.smartlogistics.ticker.SmartStockTickerBlockEntity;
import io.github.iksanbro.smartlogistics.ticker.SmartStockTickerBlockEntity.ProcessingEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

@GameTestHolder(SmartLogistics.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SmartLogisticsGameTests {

    private static ItemStack box(String address, ItemStack... contents) {
        ItemStackHandler h = new ItemStackHandler(PackageItem.SLOTS);
        for (int i = 0; i < contents.length; i++) h.setStackInSlot(i, contents[i]);
        ItemStack box = PackageItem.containing(h);
        PackageItem.addAddress(box, address);
        return box;
    }

    private static void log(String text, Object... args) {
        SmartLogistics.LOGGER.info("[test] " + text, args);
    }

    /** Create sees the smart blocks as its own frogport / packager / stock ticker. */
    @GameTest(template = "area")
    public static void aliasesAndAddresses(GameTestHelper helper) {
        helper.assertTrue(AllBlocks.PACKAGE_FROGPORT.has(ModBlocks.SMART_FROGPORT.get().defaultBlockState()), "frogport block alias");
        helper.assertTrue(AllBlocks.PACKAGE_FROGPORT.isIn(new ItemStack(ModBlocks.SMART_FROGPORT.get())), "frogport item alias");
        helper.assertTrue(AllBlocks.PACKAGER.has(ModBlocks.SMART_PACKAGER.get().defaultBlockState()), "packager alias");
        helper.assertTrue(AllBlocks.STOCK_TICKER.has(ModBlocks.SMART_STOCK_TICKER.get().defaultBlockState()), "ticker alias");
        helper.assertFalse(AllBlocks.PACKAGER.has(ModBlocks.SMART_FROGPORT.get().defaultBlockState()), "no wrong alias");

        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, ModBlocks.SMART_FROGPORT.get());
        SmartFrogportBlockEntity port = (SmartFrogportBlockEntity) helper.getBlockEntity(pos);
        helper.assertTrue(AllBlockEntityTypes.PACKAGE_FROGPORT.is(port), "frogport block entity alias");

        port.applySettings(List.of("Iron", "Gold*", " ", "Iron"), SmartFrogportBlockEntity.Mode.BOTH);
        String filter = port.getFilterString();
        helper.assertTrue(port.addresses.equals(List.of("Iron", "Gold*")), "addresses cleaned: " + port.addresses);
        helper.assertTrue(PackageItem.matchAddress("Iron", filter), "first address");
        helper.assertTrue(PackageItem.matchAddress("Gold Farm", filter), "wildcard address");
        helper.assertFalse(PackageItem.matchAddress("Copper", filter), "other address");
        helper.assertTrue(AddressList.encode(List.of("One")).equals("One"), "one address stays plain");

        port.applySettings(List.of("Iron"), SmartFrogportBlockEntity.Mode.SEND_ONLY);
        helper.assertTrue(port.getFilterString() == null, "send only takes nothing from the chain");
        helper.succeed();
    }

    /** A smart frogport catches 5 packages at once, the 6th waits; all land in its inventory. */
    @GameTest(template = "area", timeoutTicks = 60)
    public static void frogportCatchesFive(GameTestHelper helper) {
        BlockPos pos = new BlockPos(6, 1, 2);
        helper.setBlock(pos, ModBlocks.SMART_FROGPORT.get());
        SmartFrogportBlockEntity port = (SmartFrogportBlockEntity) helper.getBlockEntity(pos);
        int caught = 0;
        for (int i = 0; i < 6; i++) if (port.tryCatch(box("Here", new ItemStack(Items.DIRT, i + 1)))) caught++;
        helper.assertTrue(caught == 5, "caught " + caught + " of 6 at once");
        helper.runAfterDelay(20, () -> {
            int stored = 0;
            for (int i = 0; i < port.inventory.getSlots(); i++) if (!port.inventory.getStackInSlot(i).isEmpty()) stored++;
            helper.assertTrue(stored == 5, "packages in the frogport: " + stored);
            port.applySettings(List.of("Here"), SmartFrogportBlockEntity.Mode.SEND_ONLY);
            helper.assertFalse(port.tryCatch(box("Here")), "send only catches nothing");
            helper.succeed();
        });
    }

    /** Auto-packaging packs everything at once (up to 5 boxes), with the address from the menu. */
    @GameTest(template = "area", timeoutTicks = 60)
    public static void packagerPacksSeveralBoxes(GameTestHelper helper) {
        BlockPos chest = new BlockPos(2, 1, 6);
        BlockPos packager = new BlockPos(3, 1, 6);
        helper.setBlock(chest, Blocks.CHEST);
        ChestBlockEntity chestBe = (ChestBlockEntity) helper.getBlockEntity(chest);
        for (int i = 0; i < 27; i++) chestBe.setItem(i, new ItemStack(Items.IRON_SWORD)); // 9 per box: 3 boxes
        helper.setBlock(packager, ModBlocks.SMART_PACKAGER.get().defaultBlockState().setValue(PackagerBlock.FACING, Direction.EAST));
        SmartPackagerBlockEntity be = (SmartPackagerBlockEntity) helper.getBlockEntity(packager);
        be.applySettings("Armory", true);
        helper.runAfterDelay(25, () -> {
            log("smart packager made {} boxes, chest empty: {}", be.finishedBoxes(), chestBe.isEmpty());
            helper.assertTrue(be.finishedBoxes() == 3, "finished boxes: " + be.finishedBoxes());
            helper.assertTrue(chestBe.isEmpty(), "everything packed");
            IItemHandler out = be.smartInventory;
            for (int i = 0; i < out.getSlots(); i++) {
                ItemStack b = out.getStackInSlot(i);
                if (!b.isEmpty()) helper.assertTrue("Armory".equals(PackageItem.getAddress(b)), "address " + PackageItem.getAddress(b));
            }
            helper.succeed();
        });
    }

    /** 5 boxes go into a smart packager in one tick and are unpacked; Create's packager takes one. */
    @GameTest(template = "area", timeoutTicks = 60)
    public static void packagerUnpacksFive(GameTestHelper helper) {
        BlockPos chest = new BlockPos(8, 1, 6);
        BlockPos smart = new BlockPos(9, 1, 6);
        BlockPos chest2 = new BlockPos(8, 1, 9);
        BlockPos plain = new BlockPos(9, 1, 9);
        helper.setBlock(chest, Blocks.CHEST);
        helper.setBlock(chest2, Blocks.CHEST);
        helper.setBlock(smart, ModBlocks.SMART_PACKAGER.get().defaultBlockState().setValue(PackagerBlock.FACING, Direction.EAST));
        helper.setBlock(plain, AllBlocks.PACKAGER.get().defaultBlockState().setValue(PackagerBlock.FACING, Direction.EAST));
        helper.runAfterDelay(2, () -> {
            SmartPackagerBlockEntity smartBe = (SmartPackagerBlockEntity) helper.getBlockEntity(smart);
            PackagerBlockEntity plainBe = (PackagerBlockEntity) helper.getBlockEntity(plain);
            int smartTook = 0, plainTook = 0;
            for (int i = 0; i < 6; i++) {
                if (smartBe.smartInventory.insertItem(0, box("", new ItemStack(Items.COBBLESTONE, 10)), false).isEmpty()) smartTook++;
                if (plainBe.inventory.insertItem(0, box("", new ItemStack(Items.COBBLESTONE, 10)), false).isEmpty()) plainTook++;
            }
            log("boxes unpacked in one tick: smart packager {}, Create's packager {}", smartTook, plainTook);
            helper.assertTrue(smartTook == 5, "smart packager unpacked " + smartTook);
            ChestBlockEntity c = (ChestBlockEntity) helper.getBlockEntity(chest);
            int stone = 0;
            for (int i = 0; i < c.getContainerSize(); i++) stone += c.getItem(i).getCount();
            helper.assertTrue(stone == 50, "cobblestone in the chest: " + stone);
            helper.succeed();
        });
    }

    /** Crafts go to the crafting address, processing recipes to their type's address, the rest as typed. */
    @GameTest(template = "area", timeoutTicks = 80)
    public static void tickerSplitsOrders(GameTestHelper helper) {
        BlockPos chest = new BlockPos(14, 1, 2);
        BlockPos packager = new BlockPos(15, 1, 2);
        BlockPos link = new BlockPos(15, 2, 2);
        BlockPos ticker = new BlockPos(18, 1, 2);
        helper.setBlock(chest, Blocks.CHEST);
        ChestBlockEntity chestBe = (ChestBlockEntity) helper.getBlockEntity(chest);
        chestBe.setItem(0, new ItemStack(Items.IRON_INGOT, 32));
        chestBe.setItem(1, new ItemStack(Items.GOLD_INGOT, 32));
        chestBe.setItem(2, new ItemStack(Items.RAW_COPPER, 32));
        helper.setBlock(packager, AllBlocks.PACKAGER.get().defaultBlockState().setValue(PackagerBlock.FACING, Direction.EAST));
        helper.setBlock(link, AllBlocks.STOCK_LINK.get().defaultBlockState()
                .setValue(FaceAttachedHorizontalDirectionalBlock.FACE, AttachFace.FLOOR));
        helper.setBlock(ticker, ModBlocks.SMART_STOCK_TICKER.get());

        helper.runAfterDelay(3, () -> {
            UUID network = UUID.randomUUID();
            PackagerLinkBlockEntity linkBe = (PackagerLinkBlockEntity) helper.getBlockEntity(link);
            SmartStockTickerBlockEntity tickerBe = (SmartStockTickerBlockEntity) helper.getBlockEntity(ticker);
            for (LogisticallyLinkedBehaviour b : List.of(linkBe.behaviour, tickerBe.behaviour)) {
                LogisticallyLinkedBehaviour.remove(b);
                b.freqId = network;
                LogisticallyLinkedBehaviour.keepAlive(b);
            }
            tickerBe.applySettings("Crafters", Map.of("create:crushing", "Crusher"));

            // copper to crush, iron for a craft, gold just ordered
            List<BigItemStack> stacks = List.of(new BigItemStack(new ItemStack(Items.RAW_COPPER), 4),
                    new BigItemStack(new ItemStack(Items.IRON_INGOT), 3), new BigItemStack(new ItemStack(Items.GOLD_INGOT), 2));
            List<BigItemStack> pattern = new ArrayList<>();
            for (int i = 0; i < 9; i++) pattern.add(new BigItemStack(i < 3 ? new ItemStack(Items.IRON_INGOT) : ItemStack.EMPTY, 1));
            PackageOrderWithCrafts order = new PackageOrderWithCrafts(new PackageOrder(stacks),
                    List.of(new CraftingEntry(new PackageOrder(pattern), 1)));
            boolean sent = tickerBe.broadcastWithProcessing(order, "Storage",
                    List.of(new ProcessingEntry("create:crushing", List.of(new BigItemStack(new ItemStack(Items.RAW_COPPER), 4)))));
            helper.assertTrue(sent, "order sent");
        });

        helper.runAfterDelay(70, () -> {
            PackagerBlockEntity p = (PackagerBlockEntity) helper.getBlockEntity(packager);
            List<ItemStack> boxes = new ArrayList<>();
            if (!p.heldBox.isEmpty()) boxes.add(p.heldBox);
            for (BigItemStack q : p.queuedExitingPackages) for (int i = 0; i < q.count; i++) boxes.add(q.stack);
            List<String> seen = new ArrayList<>();
            boolean copperToCrusher = false, ironToCrafters = false, goldToStorage = false;
            for (ItemStack b : boxes) {
                String address = PackageItem.getAddress(b);
                for (ItemStack content : net.minecraft.core.NonNullList.copyOf(itemsOf(b))) {
                    if (content.isEmpty()) continue;
                    seen.add(address + ":" + content.getCount() + "x" + content.getItem());
                    if (content.is(Items.RAW_COPPER)) copperToCrusher = "Crusher".equals(address) && content.getCount() == 4;
                    if (content.is(Items.IRON_INGOT)) ironToCrafters = "Crafters".equals(address) && content.getCount() == 3;
                    if (content.is(Items.GOLD_INGOT)) goldToStorage = "Storage".equals(address) && content.getCount() == 2;
                }
            }
            log("ticker boxes: {}", seen);
            helper.assertTrue(copperToCrusher && ironToCrafters && goldToStorage, "boxes: " + seen);
            helper.succeed();
        });
    }

    private static List<ItemStack> itemsOf(ItemStack box) {
        ItemStackHandler h = PackageItem.getContents(box);
        List<ItemStack> out = new ArrayList<>();
        for (int i = 0; i < h.getSlots(); i++) out.add(h.getStackInSlot(i));
        return out;
    }

    /** A 100 x 100 grid survives the compact encoding. */
    @GameTest(template = "area")
    public static void bigGridEncoding(GameTestHelper helper) {
        List<ItemStack> grid = new ArrayList<>();
        for (int i = 0; i < 100 * 100; i++)
            grid.add(i % 7 == 0 ? ItemStack.EMPTY : new ItemStack(i % 3 == 0 ? Items.IRON_INGOT : Items.GOLD_INGOT));
        var tag = GridCodec.toTag(grid, helper.getLevel().registryAccess());
        List<ItemStack> back = GridCodec.fromTag(tag, helper.getLevel().registryAccess());
        helper.assertTrue(back.size() == grid.size(), "size");
        for (int i = 0; i < grid.size(); i++)
            helper.assertTrue(ItemStack.isSameItemSameComponents(grid.get(i), back.get(i)) || grid.get(i).isEmpty() && back.get(i).isEmpty(), "cell " + i);
        helper.assertTrue(GridCodec.side(back.size()) == 100, "side");
        helper.succeed();
    }

    // factory gauges and unloaded parts of the network

    @GameTest(template = "area", timeoutTicks = 80)
    public static void gaugeIgnoresUnloadedLinks(GameTestHelper helper) {
        gaugeTest(helper, true);
    }

    /** With the fix off the gauge waits ("?"), as in Create. Starts after the test above. */
    @GameTest(template = "area", setupTicks = 100, timeoutTicks = 80)
    public static void gaugeWaitsWithoutFix(GameTestHelper helper) {
        gaugeTest(helper, false);
    }

    private static void gaugeTest(GameTestHelper helper, boolean fix) {
        BlockPos pos = new BlockPos(2, 1, 12);
        helper.setBlock(pos, AllBlocks.FACTORY_GAUGE.get().defaultBlockState()
                .setValue(com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock.FACE, AttachFace.FLOOR));
        boolean old = Config.GAUGES_IGNORE_UNLOADED.get();
        if (!fix) Config.GAUGES_IGNORE_UNLOADED.set(false);
        UUID network = UUID.randomUUID();
        // a stock link of this network far away, in a chunk nobody loads
        var farLink = net.minecraft.core.GlobalPos.of(helper.getLevel().dimension(), new BlockPos(29_000_000, 64, 29_000_000));
        com.simibubi.create.Create.LOGISTICS.linkAdded(network, farLink, null);
        var fpbe = (com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity) helper.getBlockEntity(pos);
        var panel = fpbe.panels.get(com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock.PanelSlot.TOP_LEFT);
        panel.enable();
        panel.network = network;
        panel.setFilter(new ItemStack(Items.DIAMOND));
        fpbe.notifyUpdate();
        helper.runAfterDelay(60, () -> {
            Config.GAUGES_IGNORE_UNLOADED.set(old);
            com.simibubi.create.Create.LOGISTICS.linkRemoved(network, farLink);
            log("gauge with an unloaded link, fix {}: waiting for the network (\"?\") = {}", fix ? "on" : "off", panel.waitingForNetwork);
            helper.assertTrue(panel.waitingForNetwork != fix, "waiting = " + panel.waitingForNetwork);
            helper.succeed();
        });
    }

    // chain conveyor jam

    @GameTest(template = "area", timeoutTicks = 1000)
    public static void chainConveyorsDoNotJam(GameTestHelper helper) {
        chainTest(helper, 24, true);
    }

    /** The same with the fix off: Create's conveyors stop. Starts after the other tests. */
    @GameTest(template = "area", setupTicks = 120, timeoutTicks = 1000)
    public static void chainConveyorsJamWithoutFix(GameTestHelper helper) {
        chainTest(helper, 36, false);
    }

    private static void chainTest(GameTestHelper helper, int baseX, boolean fix) {
        BlockPos a = new BlockPos(baseX, 2, 6), b = new BlockPos(baseX + 8, 2, 6);
        BlockPos portA = a.south(2), portB = b.south(2);
        for (BlockPos c : List.of(a, b)) {
            helper.setBlock(c.below(), AllBlocks.CREATIVE_MOTOR.get().defaultBlockState().setValue(CreativeMotorBlock.FACING, Direction.UP));
            helper.setBlock(c, AllBlocks.CHAIN_CONVEYOR.get());
        }
        for (BlockPos p : List.of(portA, portB)) {
            helper.setBlock(p.below(), Blocks.CHEST);
            helper.setBlock(p, AllBlocks.PACKAGE_FROGPORT.get());
        }
        boolean oldFix = Config.CHAIN_JAM_FIX.get();
        if (!fix) Config.CHAIN_JAM_FIX.set(false);

        helper.runAfterDelay(2, () -> {
            for (BlockPos m : List.of(a.below(), b.below()))
                ((CreativeMotorBlockEntity) helper.getBlockEntity(m)).generatedSpeed.setValue(128);
            ChainConveyorBlockEntity ca = (ChainConveyorBlockEntity) helper.getBlockEntity(a);
            ChainConveyorBlockEntity cb = (ChainConveyorBlockEntity) helper.getBlockEntity(b);
            BlockPos ab = b.subtract(a);
            ca.connections.add(ab);
            cb.connections.add(ab.multiply(-1));
            ca.connectionStats = null;
            cb.connectionStats = null;
            ca.notifyUpdate();
            cb.notifyUpdate();
            setPort(helper, portA, a, "A");
            setPort(helper, portB, b, "B");
            // each conveyor full of packages for the other one
            for (int i = 0; i < 20; i++) {
                ca.addLoopingPackage(new ChainConveyorPackage(i * 18f, box("B", new ItemStack(Items.STONE))));
                cb.addLoopingPackage(new ChainConveyorPackage(i * 18f, box("A", new ItemStack(Items.STONE))));
            }
        });

        helper.runAfterDelay(900, () -> {
            Config.CHAIN_JAM_FIX.set(oldFix);
            int deliveredA = delivered(helper, portA), deliveredB = delivered(helper, portB);
            log("chain conveyors {}: delivered to A {}, to B {} of 20 each", fix ? "with the fix" : "without the fix", deliveredA, deliveredB);
            if (fix) helper.assertTrue(deliveredA == 20 && deliveredB == 20, "delivered " + deliveredA + " / " + deliveredB);
            else helper.assertTrue(deliveredA + deliveredB < 40, "Create alone should jam here, delivered " + deliveredA + " / " + deliveredB);
            helper.succeed();
        });
    }

    private static void setPort(GameTestHelper helper, BlockPos port, BlockPos conveyor, String address) {
        FrogportBlockEntity fp = (FrogportBlockEntity) helper.getBlockEntity(port);
        BlockPos absPort = helper.absolutePos(port), absConveyor = helper.absolutePos(conveyor);
        fp.addressFilter = address;
        fp.acceptsPackages = true;
        fp.target = new ChainConveyorFrogportTarget(absConveyor.subtract(absPort), 180f, (BlockPos) null, false);
        fp.target.setup(fp, helper.getLevel(), absPort);
        fp.notifyUpdate();
    }

    private static int delivered(GameTestHelper helper, BlockPos port) {
        int n = 0;
        FrogportBlockEntity fp = (FrogportBlockEntity) helper.getBlockEntity(port);
        for (int i = 0; i < fp.inventory.getSlots(); i++) n += fp.inventory.getStackInSlot(i).getCount();
        ChestBlockEntity chest = (ChestBlockEntity) helper.getBlockEntity(port.below());
        for (int i = 0; i < chest.getContainerSize(); i++) n += chest.getItem(i).getCount();
        return n;
    }
}
