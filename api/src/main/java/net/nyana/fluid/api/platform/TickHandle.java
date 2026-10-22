package net.nyana.fluid.api.platform;

import net.momirealms.craftengine.core.block.entity.BlockEntityController;
import net.momirealms.craftengine.core.block.entity.tick.BlockEntityTicker;
import net.nyana.fluid.api.block.tick.TickGate;
import org.jetbrains.annotations.ApiStatus;

/**
 * {@link TickerBridge} 创建的门控：既是 {@link TickGate}，也给出交给 CE 的 ticker。
 */
@ApiStatus.Internal
public interface TickHandle<C extends BlockEntityController> extends TickGate {
    /**
     * 交给 CE 的 ticker，由 {@code createBlockEntityTicker} 返回。
     */
    BlockEntityTicker<C> ticker();
}
