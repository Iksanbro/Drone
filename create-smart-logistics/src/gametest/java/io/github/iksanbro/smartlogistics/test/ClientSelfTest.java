package io.github.iksanbro.smartlogistics.test;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock.PanelSlot;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelPosition;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelScreen;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.content.logistics.packager.PackagerBlock;
import io.github.iksanbro.smartlogistics.SmartLogistics;
import io.github.iksanbro.smartlogistics.client.BigGridScreen;
import io.github.iksanbro.smartlogistics.client.ClientHooks;
import io.github.iksanbro.smartlogistics.frogport.SmartFrogportBlockEntity;
import io.github.iksanbro.smartlogistics.registry.ModBlocks;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Run with -Dsmartlogistics.clientTest=true (./gradlew runClientTest under Xvfb): loads the screens
 * the client mixins change, opens the mod's screens and takes screenshots of them and of the
 * blocks in a world, then quits.
 */
@EventBusSubscriber(modid = SmartLogistics.MOD_ID, value = Dist.CLIENT)
public final class ClientSelfTest {
    private ClientSelfTest() {}

    private static int step;
    private static int titleTicks;
    private static int wait;
    private static final BlockPos BASE = new BlockPos(0, -60, 4);

    private static void log(String s, Object... a) {
        SmartLogistics.LOGGER.info("[clienttest] " + s, a);
    }

    private static void shot(String name) {
        Minecraft mc = Minecraft.getInstance();
        Screenshot.grab(mc.gameDirectory, "smartlogistics_" + name + ".png", mc.getMainRenderTarget(), c -> log("screenshot {}: {}", name, c.getString()));
    }

    private static void onServer(java.util.function.Consumer<ServerLevel> action) {
        var server = Minecraft.getInstance().getSingleplayerServer();
        server.execute(() -> {
            try {
                action.accept(server.overworld());
            } catch (Throwable t) {
                SmartLogistics.LOGGER.error("[clienttest] server task failed", t);
            }
        });
    }

    @SubscribeEvent
    public static void screenOpened(net.neoforged.neoforge.client.event.ScreenEvent.Opening event) {
        if (Boolean.getBoolean("smartlogistics.clientTest") && event.getNewScreen() != null)
            log("screen opening: {}", event.getNewScreen().getClass().getSimpleName());
    }

    /** The order menu closes without a stock keeper next to the ticker: the player stands in for one. */
    @SubscribeEvent
    public static void keeper(net.neoforged.neoforge.client.event.ScreenEvent.Init.Post event) {
        if (!Boolean.getBoolean("smartlogistics.clientTest")
                || !(event.getScreen() instanceof com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen screen))
            return;
        try {
            var field = com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen.class.getDeclaredField("stockKeeper");
            field.setAccessible(true);
            field.set(screen, new java.lang.ref.WeakReference<net.minecraft.world.entity.LivingEntity>(Minecraft.getInstance().player));
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("smartlogistics.clientTest")) return;
        Minecraft mc = Minecraft.getInstance();
        if (wait > 0) {
            wait--;
            return;
        }
        try {
            switch (step) {
                case 0 -> {
                    if (!(mc.screen instanceof TitleScreen) || mc.getOverlay() != null) return;
                    if (titleTicks++ < 40) return;
                    for (String c : List.of("com.simibubi.create.content.logistics.factoryBoard.FactoryPanelScreen",
                            "com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen"))
                        Class.forName(c);
                    log("client mixin targets loaded");
                    List<ItemStack> palette = List.of(new ItemStack(Items.IRON_INGOT), new ItemStack(Items.GOLD_INGOT), new ItemStack(Items.REDSTONE));
                    List<ItemStack> grid = new ArrayList<>();
                    for (int i = 0; i < 100; i++) grid.add(palette.get((i / 10 + i % 10) % 3));
                    mc.setScreen(new BigGridScreen(mc.screen, palette, grid, new ItemStack(Items.DIAMOND), r -> log("grid saved: {} cells", r.size())));
                    step = 1;
                    wait = 20;
                }
                case 1 -> {
                    shot("grid_alone");
                    step = 2;
                    wait = 10;
                }
                case 2 -> {
                    mc.createWorldOpenFlows().createFreshLevel("smartlogistics_test",
                            new LevelSettings("smartlogistics_test", GameType.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT),
                            new WorldOptions(1L, false, false),
                            provider -> provider.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                            new TitleScreen());
                    step = 3;
                }
                case 3 -> {
                    if (mc.level == null || mc.player == null || mc.screen != null) return;
                    onServer(level -> {
                        level.setDayTime(6000);
                        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
                        ServerPlayer p = level.getServer().getPlayerList().getPlayers().get(0);
                        p.teleportTo(level, BASE.getX() + 0.5, BASE.getY(), BASE.getZ() - 3.5, 0, 25);
                        Direction face = Direction.NORTH;
                        level.setBlockAndUpdate(BASE.offset(-3, 0, 0), AllBlocks.PACKAGE_FROGPORT.get().defaultBlockState());
                        level.setBlockAndUpdate(BASE.offset(-3, 0, 2), ModBlocks.SMART_FROGPORT.get().defaultBlockState());
                        level.setBlockAndUpdate(BASE.offset(-1, 0, 0), AllBlocks.PACKAGER.get().defaultBlockState().setValue(PackagerBlock.FACING, face));
                        level.setBlockAndUpdate(BASE.offset(-1, 0, 2), ModBlocks.SMART_PACKAGER.get().defaultBlockState().setValue(PackagerBlock.FACING, face));
                        level.setBlockAndUpdate(BASE.offset(1, 0, 0), AllBlocks.STOCK_TICKER.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
                        level.setBlockAndUpdate(BASE.offset(1, 0, 2), ModBlocks.SMART_STOCK_TICKER.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
                        level.setBlockAndUpdate(BASE.offset(3, 0, 2), Blocks.CHEST.defaultBlockState());
                        if (level.getBlockEntity(BASE.offset(-3, 0, 2)) instanceof SmartFrogportBlockEntity port)
                            port.applySettings(List.of("Farm", "Mine*", "Storage"), SmartFrogportBlockEntity.Mode.BOTH);
                        // a factory gauge with an output (diamond) and one input (iron)
                        BlockPos gauge = BASE.offset(3, 0, 0);
                        level.setBlockAndUpdate(gauge, AllBlocks.FACTORY_GAUGE.get().defaultBlockState()
                                .setValue(FactoryPanelBlock.FACE, net.minecraft.world.level.block.state.properties.AttachFace.FLOOR));
                        if (level.getBlockEntity(gauge) instanceof FactoryPanelBlockEntity fpbe) {
                            FactoryPanelBehaviour out = fpbe.panels.get(PanelSlot.TOP_LEFT);
                            FactoryPanelBehaviour in = fpbe.panels.get(PanelSlot.BOTTOM_LEFT);
                            out.enable();
                            in.enable();
                            out.setFilter(new ItemStack(Items.DIAMOND));
                            in.setFilter(new ItemStack(Items.IRON_INGOT));
                            out.addConnection(in.getPanelPosition());
                            fpbe.notifyUpdate();
                        }
                    });
                    step = 4;
                    wait = 120;
                }
                case 4 -> {
                    mc.player.setYRot(0);
                    mc.player.setXRot(25);
                    shot("world");
                    step = 5;
                    wait = 10;
                }
                case 5 -> {
                    ClientHooks.openTickerSettings(BASE.offset(1, 0, 2));
                    step = 6;
                    wait = 15;
                }
                case 6 -> {
                    shot("ticker");
                    mc.setScreen(null);
                    ClientHooks.openPackagerSettings(BASE.offset(-1, 0, 2));
                    step = 7;
                    wait = 15;
                }
                case 7 -> {
                    shot("packager");
                    mc.setScreen(null);
                    onServer(level -> {
                        ServerPlayer p = level.getServer().getPlayerList().getPlayers().get(0);
                        if (level.getBlockEntity(BASE.offset(-3, 0, 2)) instanceof SmartFrogportBlockEntity port)
                            p.openMenu(port, port.getBlockPos());
                    });
                    step = 8;
                    wait = 20;
                }
                case 8 -> {
                    shot("frogport");
                    log("screen open: {}", mc.screen == null ? "none" : mc.screen.getClass().getName());
                    step = 9;
                    wait = 10;
                }
                case 9 -> {
                    mc.player.closeContainer();
                    FactoryPanelBehaviour out = FactoryPanelBehaviour.at(mc.level, new FactoryPanelPosition(BASE.offset(3, 0, 0), PanelSlot.TOP_LEFT));
                    mc.setScreen(new FactoryPanelScreen(out));
                    step = 10;
                    wait = 10;
                }
                case 10 -> {
                    shot("gauge");
                    // press the grid button the mixin added
                    for (var child : mc.screen.children())
                        if (child instanceof IconButton b && b.getToolTip().stream().anyMatch(c -> c.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t
                                && t.getKey().equals("smartlogistics.gui.gauge.grid_button")))
                            b.onClick(b.getX() + 1, b.getY() + 1);
                    log("screen after the grid button: {}", mc.screen.getClass().getSimpleName());
                    step = 11;
                    wait = 5;
                }
                case 11 -> {
                    // 3x3 -> 5x5 -> 10x10, fill with the first input, done
                    press(mc, "smartlogistics.gui.gauge.size");
                    press(mc, "smartlogistics.gui.gauge.size");
                    press(mc, "smartlogistics.gui.gauge.fill");
                    step = 12;
                    wait = 5;
                }
                case 12 -> {
                    shot("gauge_grid_editor");
                    press(mc, "gui.done");
                    step = 13;
                    wait = 10;
                }
                case 13 -> {
                    shot("gauge_big_grid");
                    mc.setScreen(null); // sends the settings and the grid
                    step = 14;
                    wait = 20;
                }
                case 14 -> {
                    onServer(level -> {
                        if (level.getBlockEntity(BASE.offset(3, 0, 0)) instanceof FactoryPanelBlockEntity fpbe) {
                            var arrangement = fpbe.panels.get(PanelSlot.TOP_LEFT).activeCraftingArrangement;
                            long iron = arrangement.stream().filter(st -> st.is(Items.IRON_INGOT)).count();
                            log("server gauge grid: {} cells, {} iron", arrangement.size(), iron);
                        }
                    });
                    step = 15;
                    wait = 10;
                }
                case 15 -> {
                    // a stock network: chest with ten kinds of items, packager + stock link, the smart ticker
                    onServer(level -> {
                        BlockPos chest = BASE.offset(5, 0, 2), packager = BASE.offset(6, 0, 2), link = packager.above();
                        level.setBlockAndUpdate(chest, Blocks.CHEST.defaultBlockState());
                        if (level.getBlockEntity(chest) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity c) {
                            var items = List.of(Items.IRON_INGOT, Items.GOLD_INGOT, Items.COPPER_INGOT, Items.REDSTONE, Items.LAPIS_LAZULI,
                                    Items.DIAMOND, Items.EMERALD, Items.COAL, Items.QUARTZ, Items.AMETHYST_SHARD);
                            for (int i = 0; i < items.size(); i++) c.setItem(i, new ItemStack(items.get(i), 16));
                        }
                        level.setBlockAndUpdate(packager, AllBlocks.PACKAGER.get().defaultBlockState().setValue(PackagerBlock.FACING, Direction.EAST));
                        // a blaze burner next to the ticker serves as its stock keeper
                        level.setBlockAndUpdate(BASE.offset(1, 0, 3), AllBlocks.BLAZE_BURNER.get().defaultBlockState());
                        level.setBlockAndUpdate(link, AllBlocks.STOCK_LINK.get().defaultBlockState()
                                .setValue(net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock.FACE,
                                        net.minecraft.world.level.block.state.properties.AttachFace.FLOOR));
                    });
                    step = 16;
                    wait = 5;
                }
                case 16 -> {
                    onServer(level -> {
                        BlockPos tickerPos = BASE.offset(1, 0, 2);
                        var network = java.util.UUID.randomUUID();
                        var linkBe = (com.simibubi.create.content.logistics.packagerLink.PackagerLinkBlockEntity) level.getBlockEntity(BASE.offset(6, 1, 2));
                        var ticker = (io.github.iksanbro.smartlogistics.ticker.SmartStockTickerBlockEntity) level.getBlockEntity(tickerPos);
                        for (var b : List.of(linkBe.behaviour, ticker.behaviour)) {
                            com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour.remove(b);
                            b.freqId = network;
                            com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour.keepAlive(b);
                        }
                        ticker.applySettings("Crafters", java.util.Map.of("create:crushing", "Crusher", "create:milling", "Mill"));
                        ServerPlayer p = level.getServer().getPlayerList().getPlayers().get(0);
                        // the same extra data as Create's stock keeper: admin, locked, position
                        log("opening the order menu of {}", ticker);
                        p.openMenu(ticker.new RequestMenuProvider(), buf -> {
                            buf.writeBoolean(true);
                            buf.writeBoolean(false);
                            buf.writeBlockPos(tickerPos);
                        });
                    });
                    step = 17;
                    wait = 60;
                }
                case 17 -> {
                    if (!(mc.screen instanceof com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen screen)) {
                        log("no stock keeper menu: {}, container {}", mc.screen, mc.player.containerMenu);
                        step = 30;
                        return;
                    }
                    var panel = ((io.github.iksanbro.smartlogistics.client.TabHolder) screen).smartlogistics$panel();
                    log("stock keeper menu open, address tab present: {}", panel != null);
                    if (panel != null) panel.setOpen(true);
                    var recipe = mc.level.getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.parse("smartlogistics:test_ten_kinds")).orElseThrow().value();
                    var cbis = new com.simibubi.create.content.logistics.stockTicker.CraftableBigItemStack(recipe.getResultItem(mc.level.registryAccess()), recipe);
                    screen.recipesToOrder.add(cbis);
                    screen.requestCraftable(cbis, 1);
                    log("ordered for a recipe with ten kinds of ingredients: {} kinds in the order, {} crafted", screen.itemsToOrder.size(), cbis.count);
                    step = 18;
                    wait = 10;
                }
                case 18 -> {
                    shot("keeper_tab");
                    try {
                        var send = com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen.class.getDeclaredMethod("sendIt");
                        send.setAccessible(true);
                        send.invoke(mc.screen);
                    } catch (ReflectiveOperationException e) {
                        throw new RuntimeException(e);
                    }
                    step = 19;
                    wait = 30;
                }
                case 19 -> {
                    onServer(level -> {
                        var p = (com.simibubi.create.content.logistics.packager.PackagerBlockEntity) level.getBlockEntity(BASE.offset(6, 0, 2));
                        List<ItemStack> boxes = new ArrayList<>();
                        if (!p.heldBox.isEmpty()) boxes.add(p.heldBox);
                        for (var q : p.queuedExitingPackages) for (int i = 0; i < q.count; i++) boxes.add(q.stack);
                        int kinds = 0;
                        boolean recipe = false;
                        List<String> addresses = new ArrayList<>();
                        for (ItemStack b : boxes) {
                            addresses.add(com.simibubi.create.content.logistics.box.PackageItem.getAddress(b));
                            var contents = com.simibubi.create.content.logistics.box.PackageItem.getContents(b);
                            for (int i = 0; i < contents.getSlots(); i++) if (!contents.getStackInSlot(i).isEmpty()) kinds++;
                            var ctx = com.simibubi.create.content.logistics.box.PackageItem.getOrderContext(b);
                            if (ctx != null && !ctx.orderedCrafts().isEmpty()) recipe = true;
                        }
                        log("server got the order: {} boxes to {}, {} kinds of items, recipe attached {}", boxes.size(), addresses, kinds, recipe);
                    });
                    mc.setScreen(null);
                    step = 30;
                    wait = 10;
                }
                case 30 -> {
                    log("done");
                    mc.stop();
                    step = 31;
                }
                default -> {
                }
            }
        } catch (Throwable t) {
            SmartLogistics.LOGGER.error("[clienttest] failed at step {}", step, t);
            mc.stop();
            step = 99;
        }
    }

    /** Presses the button whose label is the translation {@code key}, in whatever language the client runs. */
    private static void press(Minecraft mc, String label) {
        for (var child : mc.screen.children())
            if (child instanceof net.minecraft.client.gui.components.Button b
                    && b.getMessage().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t && t.getKey().equals(label)) {
                b.onPress();
                return;
            }
        log("no button {}", label);
    }

    @SuppressWarnings("unused")
    private static File dir() {
        return Minecraft.getInstance().gameDirectory;
    }
}
