package net.nyana.fluid.api.storage.base;

import net.nyana.fluid.api.fluid.FluidStack;
import net.nyana.fluid.api.fluid.FluidVariant;
import net.nyana.fluid.api.internal.Preconditions;
import net.nyana.fluid.api.storage.FluidSlot;
import net.nyana.fluid.api.transaction.SnapshotJournal;
import net.nyana.fluid.api.transaction.TransactionContext;

import java.util.Objects;

/**
 * 单槽储罐, 直接可用, 也可以继承来加过滤、按流体改变容量、监听变化,.
 *
 * <p>默认规则：只接受已注册的流体（注销后已有的流体照常可以抽出）；同一时刻只装一种变体；
 * 每次根事务提交、内容确实变化后, {@link #version()} 加 1 并调用一次 {@link #onChanged()},.
 */
public class SingleFluidTank extends SnapshotJournal<FluidStack> implements FluidSlot {
    private final long capacity;
    private FluidStack stack = FluidStack.EMPTY;
    private long version;

    public SingleFluidTank(long capacity) {
        if (capacity < 0) throw new IllegalArgumentException("Capacity must not be negative");
        this.capacity = capacity;
    }

    /**
     * 是否接受插入某个变体,.默认只接受已注册的流体,.
     */
    protected boolean canInsert(FluidVariant resource) {
        return resource.fluid().isPresent();
    }

    /**
     * 是否允许抽取某个变体,.默认允许,.
     */
    protected boolean canExtract(FluidVariant resource) {
        return true;
    }

    /**
     * 对某个变体的容量,.默认等于构造时给定的容量, 子类可以按流体返回不同的值,.
     */
    @Override
    public long capacityFor(FluidVariant resource) {
        return this.capacity;
    }

    /**
     * 根事务提交、内容确实变化后调用一次,.默认什么也不做,.
     */
    protected void onChanged() {
    }

    @Override
    public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        Preconditions.checkResource(resource, maxAmount);
        if (maxAmount == 0 || !this.canInsert(resource)) return 0;
        if (!this.stack.isEmpty() && !this.stack.variant().equals(resource)) return 0;
        long space = this.capacityFor(resource) - this.stack.amount();
        long inserted = Math.min(maxAmount, space);
        if (inserted <= 0) return 0;
        this.updateSnapshots(transaction);
        this.stack = this.stack.isEmpty() ? FluidStack.of(resource, inserted) : this.stack.grow(inserted);
        return inserted;
    }

    @Override
    public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        Preconditions.checkResource(resource, maxAmount);
        if (maxAmount == 0 || this.stack.isEmpty() || !this.stack.variant().equals(resource) || !this.canExtract(resource))
            return 0;
        long extracted = Math.min(maxAmount, this.stack.amount());
        this.updateSnapshots(transaction);
        this.stack = this.stack.shrink(extracted);
        return extracted;
    }

    @Override
    public FluidVariant variant() {
        return this.stack.variant();
    }

    @Override
    public long amount() {
        return this.stack.amount();
    }

    @Override
    public long capacity() {
        return this.stack.isEmpty() ? this.capacity : this.capacityFor(this.stack.variant());
    }

    @Override
    public long version() {
        return this.version;
    }

    public FluidStack stack() {
        return this.stack;
    }

    /**
     * 直接设置内容, 不经过事务、不触发 {@link #onChanged()}、不改变 {@link #version()},.只在初始化或加载持久化数据时使用,
     * 不要在打开的事务里调用,.
     */
    public void setStack(FluidStack stack) {
        this.stack = Objects.requireNonNull(stack, "stack");
    }

    @Override
    protected final FluidStack createSnapshot() {
        return this.stack;
    }

    @Override
    protected final void revertToSnapshot(FluidStack snapshot) {
        this.stack = snapshot;
    }

    @Override
    protected void onRootCommit(FluidStack originalState) {
        if (!originalState.equals(this.stack)) {
            this.version++;
            this.onChanged();
        }
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[" + this.stack + " / " + this.capacity + "]";
    }
}
