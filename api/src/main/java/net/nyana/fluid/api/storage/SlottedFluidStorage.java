package net.nyana.fluid.api.storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 可选能力：有固定槽位的存储。调用方用 {@code instanceof SlottedFluidStorage} 判断。
 */
public interface SlottedFluidStorage extends FluidStorage {
    int slotCount();

    /**
     * 取一个槽。
     *
     * @throws IndexOutOfBoundsException 下标越界
     */
    FluidSlot slot(int index);

    default List<FluidSlot> slots() {
        List<FluidSlot> slots = new ArrayList<>(this.slotCount());
        for (int i = 0; i < this.slotCount(); i++) slots.add(this.slot(i));
        return Collections.unmodifiableList(slots);
    }
}
