package net.nyana.fluid.api.storage;

import net.nyana.fluid.api.fluid.FluidStack;
import net.nyana.fluid.api.fluid.FluidVariant;

import java.util.Objects;

/**
 * 一次转移的结果：状态和转移了什么。要不要、怎么告诉玩家由调用方按 {@link #status()} 决定。
 *
 * <p>这是 final class 而不是 record，以后可以加字段。
 */
public final class TransferResult {
    public static TransferResult moved(FluidVariant variant, long amount) {
        if (amount <= 0) throw new IllegalArgumentException("A successful transfer moves a positive amount");
        return new TransferResult(Status.MOVED, FluidStack.of(variant, amount));
    }

    public static TransferResult failure(Status status) {
        if (status == Status.MOVED)
            throw new IllegalArgumentException("Use TransferResult.moved for successful transfers");
        return new TransferResult(status, FluidStack.EMPTY);
    }

    private final Status status;
    private final FluidStack moved;

    private TransferResult(Status status, FluidStack moved) {
        this.status = Objects.requireNonNull(status, "status");
        this.moved = Objects.requireNonNull(moved, "moved");
    }

    public Status status() {
        return this.status;
    }

    /**
     * 转移的数量；失败时为 0。
     */
    public long amount() {
        return this.moved.amount();
    }

    /**
     * 转移的变体；失败时为空变体。
     */
    public FluidVariant variant() {
        return this.moved.variant();
    }

    /**
     * 转移的流体；失败时为空堆。
     */
    public FluidStack moved() {
        return this.moved;
    }

    public boolean success() {
        return this.status == Status.MOVED;
    }

    @Override
    public String toString() {
        return "TransferResult[" + this.status + ", " + this.moved + "]";
    }

    /**
     * 转移状态。switch 时必须带 default 分支，以后会增加新的状态。
     */
    public enum Status {
        /**
         * 成功转移了大于 0 的数量。
         */
        MOVED,
        /**
         * 源头没有符合条件的流体。
         */
        SOURCE_EMPTY,
        REJECTED,
        /**
         * 两边不在同一个 Folia 区域，或者在错误的线程上调用。
         */
        WRONG_REGION,
        /**
         * 某一边的数据被锁定。
         */
        LOCKED_DATA,
        /**
         * 某一边不支持这个方向的操作。
         */
        UNSUPPORTED,
        /**
         * 试算和执行之间存储发生了变化。
         */
        STALE,
        /**
         * 被事件监听器取消。
         */
        CANCELLED
    }
}
