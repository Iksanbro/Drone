package io.github.iksanbro.cbcparts;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Connects cannon parts placed in survival.
 *
 * <p>Every non-big cannon family of CBC and its addons has a block item that, after placing,
 * calls the static {@code onPlace(Level, BlockPos)} of its block interface, but only for players
 * in creative mode or when the material has {@code connects_in_survival}. That call is what
 * joins the new block with its neighbours of the same material. Without it the cannon fails to
 * assemble with "Cannon must have exactly one opening and exactly one closed end".
 *
 * <p>This class makes the same call for survival placements. The interfaces are looked up by
 * name so that the addons stay optional.
 */
public final class CannonPartConnector {
    private CannonPartConnector() {}

    private static final String[] FAMILY_INTERFACES = {
            // Create Big Cannons: autocannon barrel, breech, recoil spring
            "rbasamoyai.createbigcannons.cannons.autocannon.AutocannonBlock",
            // CBC: Advanced Technologies
            "com.dsvv.cbcat.cannon.twin_autocannon.TwinAutocannonBlock",
            "com.dsvv.cbcat.cannon.heavy_autocannon.HeavyAutocannonBlock",
            "com.dsvv.cbcat.cannon.rocketpod.RocketPodBlock",
            "com.dsvv.cbcat.cannon.medium_rocketpod.MediumRocketPodBlock",
            // CBC Neo Warfare
            "riftyboi.cbcmodernwarfare.cannons.medium_cannon.MediumcannonBlock",
            "riftyboi.cbcmodernwarfare.cannons.rotarycannon.RotarycannonBlock",
    };

    private record Family(Class<?> blockInterface, MethodHandle onPlace) {}

    private static List<Family> families;

    private static List<Family> families() {
        if (families != null) return families;
        List<Family> found = new ArrayList<>();
        MethodType type = MethodType.methodType(void.class, Level.class, BlockPos.class);
        ClassLoader loader = CannonPartConnector.class.getClassLoader();
        for (String name : FAMILY_INTERFACES) {
            try {
                Class<?> cls = Class.forName(name, false, loader);
                MethodHandle handle = MethodHandles.publicLookup().findStatic(cls, "onPlace", type);
                found.add(new Family(cls, handle));
            } catch (ClassNotFoundException e) {
                // addon is not installed
            } catch (ReflectiveOperationException | LinkageError e) {
                CBCPartsAssembly.LOGGER.warn("Cannot hook cannon family {}: {}", name, e.toString());
            }
        }
        CBCPartsAssembly.LOGGER.info("Survival cannon part connection active for {} cannon families", found.size());
        families = List.copyOf(found);
        return families;
    }

    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!Config.CONNECT_IN_SURVIVAL.get()) return;

        Entity entity = event.getEntity();
        // Creative placements are already connected by the cannon mods themselves.
        if (entity instanceof Player player && !(player instanceof FakePlayer) && player.isCreative()) return;
        boolean machine = entity == null || entity instanceof FakePlayer;
        if (machine && !Config.CONNECT_FOR_MACHINES.get()) return;

        if (event instanceof BlockEvent.EntityMultiPlaceEvent multi) {
            for (BlockSnapshot snapshot : multi.getReplacedBlockSnapshots()) {
                connect(level, snapshot.getPos());
            }
        } else {
            connect(level, event.getPos());
        }
    }

    private static void connect(ServerLevel level, BlockPos pos) {
        Block block = level.getBlockState(pos).getBlock();
        for (Family family : families()) {
            if (!family.blockInterface().isInstance(block)) continue;
            try {
                family.onPlace().invokeExact((Level) level, pos);
            } catch (Throwable t) {
                CBCPartsAssembly.LOGGER.error("Failed to connect cannon part at {}", pos, t);
            }
            return;
        }
    }
}
