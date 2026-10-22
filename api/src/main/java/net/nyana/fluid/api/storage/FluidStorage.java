package net.nyana.fluid.api.storage;

import net.nyana.fluid.api.fluid.FluidStack;
import net.nyana.fluid.api.fluid.FluidVariant;
import net.nyana.fluid.api.internal.Preconditions;
import net.nyana.fluid.api.storage.base.FluidStorages;
import net.nyana.fluid.api.transaction.Transaction;
import net.nyana.fluid.api.transaction.TransactionContext;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * 流体存储。实现方只需实现 insert / extract / iterator，快照交给 SnapshotJournal；
 * 多个存储要原子操作时自己开事务，转移用 {@link FluidTransfer}。Folia 上只能在所属区域线程访问。
 */
public interface FluidStorage extends Iterable<FluidSlotView> {
    /**
     * 一个永远为空、什么都不接受的存储。
     */
    static FluidStorage empty() {
        return FluidStorages.empty();
    }

    /**
     * 在事务里插入流体。
     */
    long insert(FluidVariant resource, long maxAmount, TransactionContext transaction);

    /**
     * 在事务里抽取流体。
     */
    long extract(FluidVariant resource, long maxAmount, TransactionContext transaction);

    /**
     * 遍历全部槽，包括空槽。
     */
    @Override
    Iterator<FluidSlotView> iterator();

    /**
     * 是否可能接受插入。返回 {@code false} 时 {@link #insert} 一定返回 0；返回 {@code true} 不保证能插入。
     */
    default boolean supportsInsertion() {
        return true;
    }

    /**
     * 是否可能允许抽取。返回 {@code false} 时 {@link #extract} 一定返回 0。
     */
    default boolean supportsExtraction() {
        return true;
    }

    /**
     * 变化计数：跟踪变化的实现每次根事务提交后加 1，可以用来判断缓存或外观是否需要刷新。
     */
    default long version() {
        return 0;
    }

    /**
     * 数据是否被锁定。持久化数据读成 {@code UNKNOWN} 或 {@code INVALID} 时存储会被锁定：拒绝插入和抽取，原数据原样保留。
     */
    default boolean isLocked() {
        return false;
    }

    default Iterable<FluidSlotView> nonEmptyViews() {
        return () -> {
            List<FluidSlotView> views = new ArrayList<>();
            for (FluidSlotView view : this) if (!view.isBlank() && view.amount() > 0) views.add(view);
            return views.iterator();
        };
    }

    default long amountOf(FluidVariant resource) {
        Objects.requireNonNull(resource, "resource");
        long total = 0;
        for (FluidSlotView view : this)
            if (view.variant().equals(resource)) total = Math.addExact(total, view.amount());
        return total;
    }

    default long totalAmount() {
        long total = 0;
        for (FluidSlotView view : this) total = Math.addExact(total, view.amount());
        return total;
    }

    /**
     * 便捷方法：插入一个堆。内部开事务，{@link FluidAction#EXECUTE} 时才提交；已有打开的事务时嵌套在它里面。
     */
    default long fill(FluidStack stack, FluidAction action) {
        Objects.requireNonNull(stack, "stack");
        Objects.requireNonNull(action, "action");
        if (stack.isEmpty()) return 0;
        try (Transaction transaction = Transaction.open(Transaction.current())) {
            long inserted = Preconditions.checkResult(this.insert(stack.variant(), stack.amount(), transaction), stack.amount());
            if (action.execute()) transaction.commit();
            return inserted;
        }
    }

    /**
     * 便捷方法：抽取指定变体。规则同 {@link #fill}。
     */
    default FluidStack drain(FluidVariant resource, long maxAmount, FluidAction action) {
        Preconditions.checkResource(resource, maxAmount);
        Objects.requireNonNull(action, "action");
        try (Transaction transaction = Transaction.open(Transaction.current())) {
            long extracted = Preconditions.checkResult(this.extract(resource, maxAmount, transaction), maxAmount);
            if (action.execute()) transaction.commit();
            return FluidStack.of(resource, extracted);
        }
    }

    /**
     * 便捷方法：按槽的顺序，抽取第一种能抽出来的流体。规则同 {@link #fill}。
     */
    default FluidStack drainAny(long maxAmount, FluidAction action) {
        Preconditions.checkAmount(maxAmount);
        Objects.requireNonNull(action, "action");
        if (maxAmount == 0) return FluidStack.EMPTY;
        try (Transaction transaction = Transaction.open(Transaction.current())) {
            for (FluidSlotView view : this.nonEmptyViews()) {
                FluidVariant variant = view.variant();
                long extracted = Preconditions.checkResult(this.extract(variant, maxAmount, transaction), maxAmount);
                if (extracted > 0) {
                    if (action.execute()) transaction.commit();
                    return FluidStack.of(variant, extracted);
                }
            }
            return FluidStack.EMPTY;
        }
    }
}
