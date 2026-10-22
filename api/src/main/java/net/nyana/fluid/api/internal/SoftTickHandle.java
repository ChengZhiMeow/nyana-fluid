package net.nyana.fluid.api.internal;

import net.momirealms.craftengine.core.block.entity.BlockEntityController;
import net.momirealms.craftengine.core.block.entity.tick.BlockEntityTicker;
import net.nyana.fluid.api.platform.TickHandle;
import org.jetbrains.annotations.ApiStatus;

/**
 * Software gate: CE still calls the entry every tick, which only reads a boolean while asleep. Used on CE 26.9 by
 * the ce-26.9 TickerBridge, and by the standalone service. Verified on CE 26.9.2.
 */
@ApiStatus.Internal
public final class SoftTickHandle<C extends BlockEntityController> implements TickHandle<C> {
    public static <C extends BlockEntityController> SoftTickHandle<C> create(BlockEntityTicker<C> delegate) {
        return new SoftTickHandle<>(delegate);
    }

    private final BlockEntityTicker<C> ticker;
    private volatile boolean sleeping;

    private SoftTickHandle(BlockEntityTicker<C> delegate) {
        this.ticker = (world, pos, state, controller) -> {
            if (!this.sleeping) delegate.tick(world, pos, state, controller);
        };
    }

    @Override
    public BlockEntityTicker<C> ticker() {
        return this.ticker;
    }

    @Override
    public boolean awake() {
        return !this.sleeping;
    }

    @Override
    public void wake() {
        this.sleeping = false;
    }

    @Override
    public void sleep() {
        this.sleeping = true;
    }
}
