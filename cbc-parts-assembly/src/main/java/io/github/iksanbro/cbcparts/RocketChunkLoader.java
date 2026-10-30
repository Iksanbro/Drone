package io.github.iksanbro.cbcparts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Keeps rockets of CBC Enchanced Shells flying through chunks nobody has loaded.
 *
 * <p>The rockets ({@code cbc_enhanced_shells:rocket}, and {@code small_missile} in newer
 * versions) do nothing to keep their way loaded. Once one leaves the area around players it
 * freezes in mid-air (the chunk does not tick entities) or is saved away with the chunk.
 *
 * <p>For every flying rocket, every tick:
 * <ul>
 * <li>the chunks on its path for the next {@value #LOOKAHEAD_TICKS} ticks, three chunks wide,
 * get a short-lived ticket so they are loaded (or generated) before the rocket gets there and
 * the rocket is never saved away with an unloading chunk;</li>
 * <li>if the rocket is in a loaded chunk that does not tick entities (far from players), this
 * ticks it the same way the server ticks entities in ticking chunks.</li>
 * </ul>
 * Tickets expire on their own a few seconds after the rocket has passed.
 */
public final class RocketChunkLoader {
    private RocketChunkLoader() {}

    private static final String ENHANCED_SHELLS = "cbc_enhanced_shells";
    private static final Set<String> ROCKETS = Set.of("rocket", "small_missile");
    /** ticks of flight ahead whose chunks are loaded: loading takes a moment, so start early */
    private static final int LOOKAHEAD_TICKS = 40;
    /** but never further than this many blocks ahead */
    private static final double MAX_LOOKAHEAD_BLOCKS = 320;
    /** region ticket distance 1: the chunk and its 8 neighbours fully loaded */
    private static final int TICKET_DISTANCE = 1;

    private static final TicketType<ChunkPos> TICKET =
            TicketType.create("cbcparts_rocket", Comparator.comparingLong(ChunkPos::toLong), 60);

    private static final Set<Entity> FLYING = Collections.newSetFromMap(new WeakHashMap<>());
    /** tick counter of each rocket before the world ticked: if it changed, the server ticked it */
    private static final Map<Entity, Integer> TICK_COUNT_BEFORE = new WeakHashMap<>();

    public static boolean isRocket(Entity entity) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return id.getNamespace().equals(ENHANCED_SHELLS) && ROCKETS.contains(id.getPath());
    }

    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel && isRocket(event.getEntity())) FLYING.add(event.getEntity());
    }

    private static List<Entity> rocketsIn(ServerLevel level) {
        List<Entity> rockets = new ArrayList<>();
        FLYING.removeIf(Entity::isRemoved);
        for (Entity rocket : FLYING) {
            if (rocket.level() == level) rockets.add(rocket);
        }
        return rockets;
    }

    /** Before the world ticks: load the way ahead. */
    public static void onLevelTickPre(LevelTickEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel level) || FLYING.isEmpty()) return;
        if (!Config.ROCKETS_LOAD_CHUNKS.get()) return;
        for (Entity rocket : rocketsIn(level)) {
            TICK_COUNT_BEFORE.put(rocket, rocket.tickCount);
            forEachChunk(rocket.position(), rocket.getDeltaMovement(), LOOKAHEAD_TICKS,
                    chunk -> level.getChunkSource().addRegionTicket(TICKET, chunk, TICKET_DISTANCE, chunk));
        }
    }

    /** After the world ticked its entities: tick the rockets it skipped because they are far from players. */
    public static void onLevelTickPost(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || FLYING.isEmpty()) return;
        if (!Config.ROCKETS_LOAD_CHUNKS.get()) return;
        for (Entity rocket : rocketsIn(level)) {
            if (rocket.isRemoved() || rocket.isPassenger()) continue;
            Integer before = TICK_COUNT_BEFORE.remove(rocket);
            if (before == null || before != rocket.tickCount) continue; // new this tick, or the server ticked it already
            ChunkPos chunk = rocket.chunkPosition();
            if (!level.getChunkSource().hasChunk(chunk.x, chunk.z)) continue; // not loaded yet: wait
            level.guardEntityTick(level::tickNonPassenger, rocket);
        }
    }

    /** Every chunk on the straight path of the next {@code ticks} ticks, the current one included. */
    static void forEachChunk(Vec3 from, Vec3 motion, int ticks, Consumer<ChunkPos> action) {
        Vec3 ahead = motion.scale(ticks);
        if (ahead.length() > MAX_LOOKAHEAD_BLOCKS) ahead = ahead.normalize().scale(MAX_LOOKAHEAD_BLOCKS);
        Vec3 to = from.add(ahead);
        int steps = Math.max(1, (int) Math.ceil(ahead.horizontalDistance() / 8.0)); // 8 blocks: no chunk is skipped
        long last = Long.MIN_VALUE;
        for (int i = 0; i <= steps; i++) {
            ChunkPos chunk = new ChunkPos(BlockPos.containing(from.lerp(to, i / (double) steps)));
            if (chunk.toLong() == last) continue;
            last = chunk.toLong();
            action.accept(chunk);
        }
    }
}
