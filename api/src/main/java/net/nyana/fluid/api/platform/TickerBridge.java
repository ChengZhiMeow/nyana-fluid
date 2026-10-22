package net.nyana.fluid.api.platform;

import net.momirealms.craftengine.core.block.entity.BlockEntityController;
import net.momirealms.craftengine.core.block.entity.tick.BlockEntityTicker;
import net.momirealms.craftengine.core.world.BlockPos;
import net.nyana.fluid.api.Registration;
import org.jetbrains.annotations.ApiStatus;

/**
 * tick 门控的版本差异。有两个实现：{@code platform/ce-26.9} 的软门控和 {@code platform/ce-26.10} 的原生休眠
 * （{@code SleepingBlockEntityTicker}）。插件启动时探测 CE 有没有原生休眠来选择。
 */
@ApiStatus.Internal
public interface TickerBridge {
    /**
     * 包装一个 ticker，得到可以休眠的门控。
     */
    <C extends BlockEntityController> TickHandle<C> create(BlockEntityTicker<C> delegate);

    /**
     * 订阅某个位置所在区块的加载。目标区块已经加载时，第一次回调会推迟执行；回调里必须重新检查目标是否可用。
     * 订阅在方块实体卸载或移除时应当取消。
     */
    Registration subscribeChunkLoad(BlockEntityController controller, BlockPos target, Runnable callback);

    /**
     * 是否是原生休眠（CE 不再调用休眠中的 ticker）。
     */
    boolean nativeSleep();
}
