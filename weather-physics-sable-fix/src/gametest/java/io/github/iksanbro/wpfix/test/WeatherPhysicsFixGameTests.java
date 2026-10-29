package io.github.iksanbro.wpfix.test;

import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import io.github.iksanbro.wpfix.WeatherPhysicsSableFix;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.joml.Vector3d;

/** Checks the fix for the "Body has been removed" crash of Weather Physics (tornadophysics) with Sable. */
@GameTestHolder(WeatherPhysicsSableFix.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WeatherPhysicsFixGameTests {
    private static final Logger LOGGER = LogUtils.getLogger();

    @GameTest(template = "empty")
    public static void tornadoPhysicsSkipsRemovedBody(GameTestHelper helper) throws Throwable {
        if (!ModList.get().isLoaded("tornadophysics")) {
            helper.succeed();
            return;
        }
        ServerLevel level = helper.getLevel();
        ServerSubLevelContainer container = SubLevelContainer.getContainer(level);

        // a Sable sub-level with a physics body, then removed, as when a ship is destroyed mid-tick
        // MethodHandles look up single methods; Class.getMethod would touch Sable's client-only overloads
        MethodHandles.Lookup lookup = MethodHandles.publicLookup();
        Class<?> poseClass = Class.forName("dev.ryanhcode.sable.companion.math.Pose3d");
        Object pose = poseClass.getConstructor().newInstance();
        Class<?> subLevelClass = Class.forName("dev.ryanhcode.sable.sublevel.SubLevel");
        MethodHandle allocate = lookup.findVirtual(SubLevelContainer.class, "allocateNewSubLevel",
                MethodType.methodType(subLevelClass, poseClass));
        ServerSubLevel subLevel;
        try {
            subLevel = (ServerSubLevel) allocate.invoke(container, pose);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
        RigidBodyHandle handle = RigidBodyHandle.of(subLevel);
        helper.assertTrue(handle != null, "no rigid body for the new sub-level");

        Class<?> reasonClass = Class.forName("dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason");
        Object removed = lookup.findStaticGetter(reasonClass, "REMOVED", reasonClass).invoke();
        Class<?> plotClass = Class.forName("dev.ryanhcode.sable.sublevel.plot.LevelPlot");
        Object plot = lookup.findVirtual(subLevelClass, "getPlot", MethodType.methodType(plotClass)).invoke(subLevel);
        ChunkPos plotPos = (ChunkPos) lookup.findGetter(plotClass, "plotPos", ChunkPos.class).invoke(plot);
        lookup.findVirtual(ServerSubLevelContainer.class, "removeSubLevel",
                MethodType.methodType(void.class, int.class, int.class, reasonClass)).invoke(container, plotPos.x - container.getOrigin().x(), plotPos.z - container.getOrigin().y(), removed);
        container.processSubLevelRemovals();

        // the crash itself: asking a removed body for its velocity
        String crash = null;
        try {
            handle.getLinearVelocity(new Vector3d());
        } catch (RuntimeException e) {
            crash = e.getMessage();
        }
        LOGGER.info("[test] removed body: isValid={}, direct velocity call -> {}", handle.isValid(), crash);
        helper.assertTrue("Body has been removed".equals(crash), "could not reproduce the crash, got: " + crash);

        // Weather Physics on the same body must now skip it instead of crashing
        Class<?> tornadoEvent = Class.forName("net.killey.tornadophysics.event.TornadoEvent");
        Method process = null;
        for (Method m : tornadoEvent.getDeclaredMethods()) {
            if (m.getName().equals("processPhysics")) process = m;
        }
        helper.assertTrue(process != null, "TornadoEvent.processPhysics not found");
        process.setAccessible(true);
        // a real Weather2 storm, so without the fix the method gets as far as the velocity call
        Class<?> managerClass = Class.forName("weather2.weathersystem.WeatherManager");
        Class<?> stormClass = Class.forName("weather2.weathersystem.storm.StormObject");
        Object manager = lookup.findStatic(Class.forName("weather2.ServerTickHandler"), "getWeatherManagerFor",
                MethodType.methodType(Class.forName("weather2.weathersystem.WeatherManagerServer"), net.minecraft.world.level.Level.class)).invoke(level);
        Object storm = lookup.findConstructor(stormClass, MethodType.methodType(void.class, managerClass)).invoke(manager);
        lookup.findSetter(stormClass, "pos", net.minecraft.world.phys.Vec3.class).invoke(storm, new net.minecraft.world.phys.Vec3(64, 100, 64));
        try {
            process.invoke(tornadoEvent.getConstructor().newInstance(), subLevel, storm, new Vector3d(), 1.0, 1);
        } catch (java.lang.reflect.InvocationTargetException e) {
            helper.fail("processPhysics still throws: " + e.getCause());
        }
        helper.succeed();
    }
}
