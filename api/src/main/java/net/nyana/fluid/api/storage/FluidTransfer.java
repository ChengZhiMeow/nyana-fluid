package net.nyana.fluid.api.storage;

import net.nyana.fluid.api.fluid.FluidStack;
import net.nyana.fluid.api.fluid.FluidVariant;
import net.nyana.fluid.api.internal.Preconditions;
import net.nyana.fluid.api.transaction.FluidAccessException;
import net.nyana.fluid.api.transaction.Transaction;
import net.nyana.fluid.api.transaction.TransactionContext;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * 两个存储之间的转移，以及试算工具。
 *
 * <p>{@link #move} 的语义和 Fabric {@code StorageUtil.move} 相同：按源头的槽依次处理，每种流体先试算能抽多少，
 * 再在一个嵌套事务里「插入目标 → 从源头抽出同样多」，只有两边数量一致才提交。所以不会出现「源头扣了、目标没收到」。
 */
public final class FluidTransfer {
    /**
     * 从 {@code from} 向 {@code to} 转移，最多 {@code maxAmount}。
     *
     * <p>{@code parent} 为 {@code null} 时自己开根事务并在结束时提交；否则嵌套在 {@code parent} 里，由调用方决定是否提交。
     * 任意一边为 {@code null}、被锁定、或者在错误的线程 / 区域上访问时返回 0。
     */
    public static long move(@Nullable FluidStorage from, @Nullable FluidStorage to, Predicate<FluidVariant> filter,
                            long maxAmount, @Nullable TransactionContext parent) {
        Objects.requireNonNull(filter, "filter");
        Preconditions.checkAmount(maxAmount);
        if (from == null || to == null || from == to || maxAmount == 0) return 0;
        if (from.isLocked() || to.isLocked() || !from.supportsExtraction() || !to.supportsInsertion()) return 0;
        try {
            return FluidTransfer.moveUnchecked(from, to, filter, maxAmount, parent);
        } catch (FluidAccessException wrongRegion) {
            return 0;
        }
    }

    private static long moveUnchecked(FluidStorage from, FluidStorage to, Predicate<FluidVariant> filter, long maxAmount,
                                      @Nullable TransactionContext parent) {
        long total = 0;
        try (Transaction iteration = Transaction.open(parent)) {
            for (FluidSlotView view : from.nonEmptyViews()) {
                FluidVariant variant = view.variant();
                if (variant.isBlank() || !filter.test(variant)) continue;
                long remaining = maxAmount - total;
                long available;
                try (Transaction probe = Transaction.open(iteration)) {
                    available = Preconditions.checkResult(view.extract(variant, remaining, probe), remaining);
                }
                if (available == 0) continue;
                try (Transaction transfer = Transaction.open(iteration)) {
                    long accepted = Preconditions.checkResult(to.insert(variant, available, transfer), available);
                    if (accepted == 0) continue;
                    long extracted = Preconditions.checkResult(view.extract(variant, accepted, transfer), accepted);
                    if (extracted == accepted) {
                        transfer.commit();
                        total += accepted;
                    }
                }
                if (total >= maxAmount) break;
            }
            iteration.commit();
        }
        return total;
    }

    /**
     * 和 {@link #move} 相同的转移，但返回带原因的结果，适合给玩家提示。
     * 已有打开的事务时嵌套在它里面；{@link FluidAction#EXECUTE} 时才提交。
     */
    public static TransferResult tryMove(FluidStorage from, FluidStorage to, Predicate<FluidVariant> filter, long maxAmount, FluidAction action) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(filter, "filter");
        Objects.requireNonNull(action, "action");
        Preconditions.checkAmount(maxAmount);
        if (from == to) return TransferResult.failure(TransferResult.Status.UNSUPPORTED);
        if (from.isLocked() || to.isLocked()) return TransferResult.failure(TransferResult.Status.LOCKED_DATA);
        if (!from.supportsExtraction() || !to.supportsInsertion())
            return TransferResult.failure(TransferResult.Status.UNSUPPORTED);
        try (Transaction transaction = Transaction.open(Transaction.current())) {
            boolean found = false;
            for (FluidSlotView view : from.nonEmptyViews()) {
                FluidVariant variant = view.variant();
                if (variant.isBlank() || !filter.test(variant)) continue;
                found = true;
                long moved = FluidTransfer.moveUnchecked(from, to, variant::equals, maxAmount, transaction);
                if (moved > 0) {
                    if (action.execute()) transaction.commit();
                    return TransferResult.moved(variant, moved);
                }
            }
            return TransferResult.failure(found ? TransferResult.Status.REJECTED : TransferResult.Status.SOURCE_EMPTY);
        } catch (FluidAccessException wrongRegion) {
            return TransferResult.failure(TransferResult.Status.WRONG_REGION);
        }
    }

    /**
     * 试算：能插入多少。不改变任何状态。
     */
    public static long simulateInsert(FluidStorage storage, FluidVariant resource, long maxAmount, @Nullable TransactionContext parent) {
        Preconditions.checkResource(resource, maxAmount);
        try (Transaction simulation = Transaction.open(parent)) {
            return Preconditions.checkResult(storage.insert(resource, maxAmount, simulation), maxAmount);
        }
    }

    /**
     * 试算：能抽出多少。不改变任何状态。
     */
    public static long simulateExtract(FluidStorage storage, FluidVariant resource, long maxAmount, @Nullable TransactionContext parent) {
        Preconditions.checkResource(resource, maxAmount);
        try (Transaction simulation = Transaction.open(parent)) {
            return Preconditions.checkResult(storage.extract(resource, maxAmount, simulation), maxAmount);
        }
    }

    /**
     * 找到第一种能抽出来、并且满足条件的流体，以及最多能抽出多少。不改变任何状态。
     */
    public static Optional<FluidStack> findExtractable(FluidStorage storage, Predicate<FluidVariant> filter, @Nullable TransactionContext parent) {
        Objects.requireNonNull(filter, "filter");
        for (FluidSlotView view : storage.nonEmptyViews()) {
            FluidVariant variant = view.variant();
            if (variant.isBlank() || !filter.test(variant)) continue;
            long amount = FluidTransfer.simulateExtract(storage, variant, Long.MAX_VALUE, parent);
            if (amount > 0) return Optional.of(FluidStack.of(variant, amount));
        }
        return Optional.empty();
    }

    private FluidTransfer() {
    }
}
