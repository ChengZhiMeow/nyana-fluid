package net.nyana.fluid.api.platform;

import net.momirealms.craftengine.core.world.World;
import org.jetbrains.annotations.ApiStatus;

/**
 * 调度。推迟执行一律<b>不</b>同步执行：在 Paper 主线程上调用 CE {@code PlatformExecutor.run} 会立即同步执行，
 * 所以实现必须用 {@code runDelayed}。
 */
@ApiStatus.Internal
public interface SchedulerBridge {
    /**
     * 在拥有某个区块的区域上稍后执行（Paper 上是同一 tick 稍后或下一 tick）。
     */
    void runAt(World world, int chunkX, int chunkZ, Runnable task);

    /**
     * 在全局线程上稍后执行（Paper 主线程 / Folia 全局区域）。
     */
    void runGlobal(Runnable task);

    /**
     * 在拥有某个区块的区域上，{@code delayTicks} tick 后执行。
     */
    Task runLater(World world, int chunkX, int chunkZ, long delayTicks, Runnable task);

    /**
     * 已安排的任务。
     */
    interface Task {
        void cancel();
    }
}
