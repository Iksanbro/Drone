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
        server.execute(() -> action.accept(server.overworld()));
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
                        if (child instanceof IconButton b && b.getToolTip().stream().anyMatch(c -> c.getString().contains("100")))
                            b.onClick(b.getX() + 1, b.getY() + 1);
                    log("screen after the grid button: {}", mc.screen.getClass().getSimpleName());
                    step = 11;
                    wait = 5;
                }
                case 11 -> {
                    // 3x3 -> 5x5 -> 10x10, fill with the first input, done
                    press(mc, "Grid:");
                    press(mc, "Grid:");
                    press(mc, "Fill");
                    step = 12;
                    wait = 5;
                }
                case 12 -> {
                    shot("gauge_grid_editor");
                    press(mc, "Done");
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
                    log("done");
                    mc.stop();
                    step = 16;
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

    private static void press(Minecraft mc, String label) {
        for (var child : mc.screen.children())
            if (child instanceof net.minecraft.client.gui.components.Button b && b.getMessage().getString().startsWith(label)) {
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
