package net.nyana.fluid.api.storage;

import net.nyana.fluid.api.fluid.FluidVariant;

import java.util.Iterator;
import java.util.List;

/**
 * 单个槽：它本身既是一个存储，又是自己的视图。
 */
public interface FluidSlot extends FluidStorage, FluidSlotView {
    /**
     * 对某个变体的容量。默认等于 {@link #capacity()}；有的实现会对不同流体给出不同容量。
     */
    default long capacityFor(FluidVariant resource) {
        return this.capacity();
    }

    /**
     * 单槽存储的迭代器只包含自己。
     */
    @Override
    default Iterator<FluidSlotView> iterator() {
        return List.<FluidSlotView>of(this).iterator();
    }
}
