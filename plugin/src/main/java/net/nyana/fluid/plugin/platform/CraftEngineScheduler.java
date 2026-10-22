package net.nyana.fluid.plugin.platform;

import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.scheduler.PlatformExecutor;
import net.momirealms.craftengine.core.world.World;
import net.nyana.fluid.api.platform.SchedulerBridge;

public final class CraftEngineScheduler implements SchedulerBridge {
    private static PlatformExecutor executor() {
        return CraftEngine.instance().scheduler().platform();
    }

    @Override
    public void runAt(World world, int chunkX, int chunkZ, Runnable task) {
        CraftEngineScheduler.executor().runDelayed(task, world, chunkX, chunkZ);
    }

    @Override
    public void runGlobal(Runnable task) {
        CraftEngineScheduler.executor().runDelayed(task);
    }

    @Override
    public Task runLater(World world, int chunkX, int chunkZ, long delayTicks, Runnable task) {
        return CraftEngineScheduler.executor().runLater(task, delayTicks, world, chunkX, chunkZ)::cancel;
    }
}
