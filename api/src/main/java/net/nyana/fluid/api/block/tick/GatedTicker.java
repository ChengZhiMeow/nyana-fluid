package net.nyana.fluid.api.block.tick;

import net.momirealms.craftengine.core.block.entity.BlockEntityController;
import net.momirealms.craftengine.core.block.entity.tick.BlockEntityTicker;
import net.momirealms.craftengine.core.world.BlockPos;
import net.nyana.fluid.api.NyanaFluid;
import net.nyana.fluid.api.Registration;
import net.nyana.fluid.api.internal.SoftTickHandle;
import net.nyana.fluid.api.platform.PlatformBridge;
import net.nyana.fluid.api.platform.TickHandle;

import java.util.Objects;

/**
 * 可以休眠的 ticker：把业务 ticker 包起来，{@link #ticker()} 交给 CE，自己当作 {@link TickGate} 控制休眠。
 * CE 26.9 上是软门控，CE 26.10+ 上是原生休眠（由 NyanaFluid 插件按版本选择），没有安装插件时是软门控。
 */
public final class GatedTicker<C extends BlockEntityController> implements TickGate {
    /**
     * 包装一个业务 ticker。每个 controller 的每条 tick 通道只创建一个。
     */
    public static <C extends BlockEntityController> GatedTicker<C> of(BlockEntityTicker<C> delegate) {
        Objects.requireNonNull(delegate, "delegate");
        PlatformBridge bridge = NyanaFluid.service().bridge();
        return new GatedTicker<>(bridge != null ? bridge.ticker().create(delegate) : SoftTickHandle.create(delegate));
    }

    private final TickHandle<C> handle;

    private GatedTicker(TickHandle<C> handle) {
        this.handle = handle;
    }

    /**
     * 交给 CE 的 ticker。
     */
    public BlockEntityTicker<C> ticker() {
        return this.handle.ticker();
    }

    /**
     * 目标位置所在的区块加载时唤醒（例如相邻区块里的管道刚加载进来）。目标区块已经加载时，稍后唤醒一次。
     * 唤醒在 {@code owner} 的区域线程上执行。必须在 {@code owner} 的区域线程上调用（通常在 {@code onLoad} 里），
     * 方块实体卸载或移除时关闭返回的句柄。
     *
     * @throws IllegalStateException 没有安装 NyanaFluid 插件
     */
    public Registration wakeOnChunkLoad(BlockEntityController owner, BlockPos target) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(target, "target");
        PlatformBridge bridge = NyanaFluid.service().bridge();
        if (bridge == null) throw new IllegalStateException("Chunk-load wakeups need the NyanaFluid plugin");
        return bridge.ticker().subscribeChunkLoad(owner, target, this::wake);
    }

    @Override
    public boolean awake() {
        return this.handle.awake();
    }

    @Override
    public void wake() {
        this.handle.wake();
    }

    @Override
    public void sleep() {
        this.handle.sleep();
    }
}
