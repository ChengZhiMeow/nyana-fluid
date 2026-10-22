package net.nyana.fluid.api.storage;

import net.nyana.fluid.api.fluid.FluidVariant;
import net.nyana.fluid.api.transaction.TransactionContext;

/**
 * 存储里一个槽的视图：当前内容、容量，以及只从这个槽抽取的方法。
 * 视图是活的：读到的永远是当前状态。
 */
public interface FluidSlotView {
    FluidVariant variant();

    long amount();

    long capacity();

    default boolean isBlank() {
        return this.variant().isBlank();
    }

    long extract(FluidVariant resource, long maxAmount, TransactionContext transaction);

    /**
     * 穿透装饰器取到最底层的视图，用于给同一个槽去重。没有被装饰时返回自身。
     */
    default FluidSlotView underlying() {
        return this;
    }
}
