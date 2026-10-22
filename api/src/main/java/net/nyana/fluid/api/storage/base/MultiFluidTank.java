package net.nyana.fluid.api.storage.base;

import net.nyana.fluid.api.fluid.FluidVariant;
import net.nyana.fluid.api.internal.Preconditions;
import net.nyana.fluid.api.storage.FluidSlot;
import net.nyana.fluid.api.storage.FluidSlotView;
import net.nyana.fluid.api.storage.SlottedFluidStorage;
import net.nyana.fluid.api.transaction.TransactionContext;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * 多槽储罐,.每个槽是一个 {@link SingleFluidTank}, 各自只装一种变体,.
 *
 * <p>插入时默认先填已经装着同一种变体的槽, 再填空槽（{@link FillOrder#SAME_THEN_EMPTY}）；抽取时按槽的顺序抽,.
 * 用 {@link #MultiFluidTank(int, long)} 构造时, 任何一个槽变化都会调用 {@link #onChanged()}、
 * 校验 {@link #canInsert(int, FluidVariant)} 和 {@link #checkAccess()},.
 */
public class MultiFluidTank implements SlottedFluidStorage {
    private final List<SingleFluidTank> slots;
    private final long sharedCapacity;

    /**
     * 构造 {@code slots} 个容量相同的槽,.
     */
    public MultiFluidTank(int slots, long capacityEach) {
        this(slots, capacityEach, false);
    }

    /**
     * {@code shared} 为 {@code true} 时 {@code capacity} 是所有槽共用的总容量, 每种流体占一个槽,.
     */
    public MultiFluidTank(int slots, long capacity, boolean shared) {
        if (slots < 1) throw new IllegalArgumentException("A tank needs at least one slot");
        List<SingleFluidTank> created = new ArrayList<>(slots);
        for (int i = 0; i < slots; i++) created.add(new Slot(i, capacity));
        this.slots = List.copyOf(created);
        this.sharedCapacity = shared ? capacity : 0;
    }

    /**
     * 用现成的槽构造,.这些槽自己的 {@code onChanged} / {@code canInsert} 规则保持不变, 不会回调这个类的钩子,.
     */
    public MultiFluidTank(List<? extends SingleFluidTank> slots) {
        if (slots.isEmpty()) throw new IllegalArgumentException("A tank needs at least one slot");
        this.slots = List.copyOf(slots);
        this.sharedCapacity = 0;
    }

    /**
     * 总容量：共用容量时是共用的那一份, 否则是各槽容量之和,.
     */
    public long totalCapacity() {
        if (this.sharedCapacity > 0) return this.sharedCapacity;
        long total = 0;
        for (SingleFluidTank slot : this.slots) total += slot.capacity();
        return total;
    }

    /**
     * 插入时填槽的顺序,.默认 {@link FillOrder#SAME_THEN_EMPTY},.
     */
    protected FillOrder fillOrder() {
        return FillOrder.SAME_THEN_EMPTY;
    }

    /**
     * 某个槽是否接受某个变体,.默认只接受已注册的流体,.
     */
    protected boolean canInsert(int slot, FluidVariant resource) {
        return resource.fluid().isPresent();
    }

    /**
     * 任何一个槽在根事务提交后发生变化时调用；同一次提交里变化了几个槽就调用几次,.默认什么也不做,.
     */
    protected void onChanged() {
    }

    /**
     * 每次修改前的访问校验, 例如 Folia 区域归属；不满足时抛
     * {@link net.nyana.fluid.api.transaction.FluidAccessException},.默认什么也不做,.
     */
    protected void checkAccess() {
    }

    @Override
    public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        Preconditions.checkResource(resource, maxAmount);
        if (this.sharedCapacity > 0)
            maxAmount = Math.min(maxAmount, Math.max(0, this.sharedCapacity - this.totalAmount()));
        long inserted = 0;
        if (this.fillOrder() == FillOrder.SAME_THEN_EMPTY) {
            for (SingleFluidTank slot : this.slots) {
                if (inserted >= maxAmount) break;
                if (!slot.stack().isEmpty() && slot.variant().equals(resource))
                    inserted += slot.insert(resource, maxAmount - inserted, transaction);
            }
            for (SingleFluidTank slot : this.slots) {
                if (inserted >= maxAmount) break;
                if (slot.stack().isEmpty()) inserted += slot.insert(resource, maxAmount - inserted, transaction);
            }
        } else {
            for (SingleFluidTank slot : this.slots) {
                if (inserted >= maxAmount) break;
                inserted += slot.insert(resource, maxAmount - inserted, transaction);
            }
        }
        return inserted;
    }

    @Override
    public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        Preconditions.checkResource(resource, maxAmount);
        long extracted = 0;
        for (SingleFluidTank slot : this.slots) {
            if (extracted >= maxAmount) break;
            extracted += slot.extract(resource, maxAmount - extracted, transaction);
        }
        return extracted;
    }

    @Override
    public Iterator<FluidSlotView> iterator() {
        return List.<FluidSlotView>copyOf(this.slots).iterator();
    }

    @Override
    public int slotCount() {
        return this.slots.size();
    }

    @Override
    public FluidSlot slot(int index) {
        return this.slots.get(index);
    }

    /**
     * 以 {@link SingleFluidTank} 的形式取一个槽, 方便读取 {@link SingleFluidTank#stack()} 或在加载数据时 {@code setStack},.
     */
    public SingleFluidTank tank(int index) {
        return this.slots.get(index);
    }

    @Override
    public long version() {
        long version = 0;
        for (SingleFluidTank slot : this.slots) version += slot.version();
        return version;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + this.slots;
    }

    /**
     * 插入时填槽的顺序,.
     */
    public enum FillOrder {
        /**
         * 先填已经装着同一种变体的槽, 再填空槽,.
         */
        SAME_THEN_EMPTY,
        /**
         * 严格按槽的顺序填,.
         */
        IN_ORDER
    }

    private final class Slot extends SingleFluidTank {
        private final int index;

        private Slot(int index, long capacity) {
            super(capacity);
            this.index = index;
        }

        @Override
        protected boolean canInsert(FluidVariant resource) {
            return MultiFluidTank.this.canInsert(this.index, Objects.requireNonNull(resource));
        }

        @Override
        protected void checkAccess() {
            MultiFluidTank.this.checkAccess();
        }

        @Override
        protected void onChanged() {
            MultiFluidTank.this.onChanged();
        }
    }
}
