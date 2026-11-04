package io.github.iksanbro.cbcparts.test;

import io.github.iksanbro.cbcparts.CBCPartsAssembly;
import io.github.iksanbro.cbcparts.medium.MediumShells;
import java.lang.reflect.Method;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import rbasamoyai.createbigcannons.munitions.AbstractCannonProjectile;

/**
 * Run with -Dcbcparts.clientTest=true (./gradlew runClientTest under Xvfb): opens the creative tab
 * of the medium cannon shells, puts rounds and cartridges in item frames and a few rounds in the
 * air, takes screenshots (run/screenshots) and quits.
 */
@EventBusSubscriber(modid = CBCPartsAssembly.MOD_ID, value = Dist.CLIENT)
public final class MediumShellClientTest {
    private MediumShellClientTest() {}

    private static final BlockPos BASE = new BlockPos(0, -60, 6);
    private static int step;
    private static int wait;
    private static int titleTicks;

    private static void log(String s, Object... a) {
        CBCPartsAssembly.LOGGER.info("[clienttest] " + s, a);
    }

    private static void shot(String name) {
        Minecraft mc = Minecraft.getInstance();
        Screenshot.grab(mc.gameDirectory, "cbcparts_" + name + ".png", mc.getMainRenderTarget(), c -> log("screenshot {}", name));
    }

    private static void onServer(java.util.function.Consumer<ServerLevel> action) {
        var server = Minecraft.getInstance().getSingleplayerServer();
        server.execute(() -> {
            try {
                action.accept(server.overworld());
            } catch (Throwable t) {
                CBCPartsAssembly.LOGGER.error("[clienttest] server task failed", t);
            }
        });
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("cbcparts.clientTest")) return;
        Minecraft mc = Minecraft.getInstance();
        if (wait > 0) {
            wait--;
            return;
        }
        try {
            switch (step) {
                case 0 -> {
                    if (!(mc.screen instanceof TitleScreen) || mc.getOverlay() != null || titleTicks++ < 40) return;
                    if (!net.neoforged.fml.ModList.get().isLoaded("cbcmodernwarfare")) {
                        log("CBC Neo Warfare is not installed: no medium shells");
                        step = 4;
                        return;
                    }
                    mc.createWorldOpenFlows().createFreshLevel("cbcparts_test",
                            new LevelSettings("cbcparts_test", GameType.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT),
                            new WorldOptions(1L, false, false),
                            provider -> provider.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                            new TitleScreen());
                    step = 1;
                }
                case 1 -> {
                    if (mc.level == null || mc.player == null || mc.screen != null) return;
                    List<MediumShells.Entry> entries = MediumShells.entries();
                    onServer(level -> {
                        level.setDayTime(6000);
                        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
                        ServerPlayer p = level.getServer().getPlayerList().getPlayers().get(0);
                        p.teleportTo(level, BASE.getX() + 0.5, BASE.getY(), BASE.getZ() - 5.5, 0, 0);
                        // a wall of item frames: rounds in the top rows, cartridges below (some of each kind)
                        int shown = Math.min(entries.size(), 12);
                        for (int i = 0; i < shown; i++) {
                            MediumShells.Entry e = entries.get(entries.size() <= 12 ? i : i * entries.size() / shown);
                            for (int row = 0; row < 2; row++) {
                                BlockPos wall = BASE.offset(i - shown / 2, 2 - row, 1);
                                level.setBlockAndUpdate(wall, Blocks.WHITE_CONCRETE.defaultBlockState());
                                ItemFrame frame = new ItemFrame(level, wall.relative(Direction.NORTH), Direction.NORTH);
                                frame.setItem(row == 0 ? e.round().toStack() : e.cartridge().get().filled(), false);
                                level.addFreshEntity(frame);
                            }
                        }
                        // rounds hanging in the air, as they look in flight
                        for (int i = 0; i < Math.min(entries.size(), 5); i++) {
                            EntityType<?> type = entries.get(i * entries.size() / 5).entity().get();
                            if (type.create(level) instanceof AbstractCannonProjectile round) {
                                round.setPos(BASE.getX() - 2 + i, BASE.getY() + 0.3, BASE.getZ() - 2);
                                round.setNoGravity(true);
                                round.setDeltaMovement(Vec3.ZERO);
                                round.setOrientation(new Vec3(1, 0.3, 0).normalize());
                                level.addFreshEntity(round);
                            }
                        }
                    });
                    step = 2;
                    wait = 100;
                }
                case 2 -> {
                    shot("medium_shells_world");
                    CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.get(
                            ResourceLocation.fromNamespaceAndPath(CBCPartsAssembly.MOD_ID, "medium_shells"));
                    CreativeModeInventoryScreen screen = new CreativeModeInventoryScreen(mc.player,
                            mc.player.connection.enabledFeatures(), true);
                    mc.setScreen(screen);
                    Method select = CreativeModeInventoryScreen.class.getDeclaredMethod("selectTab", CreativeModeTab.class);
                    select.setAccessible(true);
                    select.invoke(screen, tab);
                    log("tab items: {}", tab.getDisplayItems().size());
                    step = 3;
                    wait = 20;
                }
                case 3 -> {
                    shot("medium_shells_tab");
                    // the tooltip of a cartridge (name, round inside)
                    ItemStack sample = MediumShells.entries().getLast().cartridge().get().filled();
                    List<net.minecraft.network.chat.Component> lines = net.minecraft.client.gui.screens.Screen.getTooltipFromItem(mc, sample);
                    log("tooltip of {}: {}", BuiltInRegistries.ITEM.getKey(sample.getItem()), lines.stream().map(net.minecraft.network.chat.Component::getString).toList());
                    mc.setScreen(null);
                    step = 4;
                    wait = 10;
                }
                case 4 -> {
                    log("done");
                    mc.stop();
                    step = 5;
                }
                default -> {
                }
            }
        } catch (Throwable t) {
            CBCPartsAssembly.LOGGER.error("[clienttest] failed at step {}", step, t);
            mc.stop();
            step = 99;
        }
    }
}
