package net.nyana.fluid.api.transaction;

import org.jetbrains.annotations.ApiStatus;

import java.util.Arrays;
import java.util.Objects;

/**
 * 事务参与者的基类,.自己实现存储时继承它：每次修改状态<b>之前</b>调用 {@link #updateSnapshots(TransactionContext)},
 * 其余的回滚、嵌套合并、根提交回调都由这个类处理,.
 */
@ApiStatus.OverrideOnly
public abstract class SnapshotJournal<S> {
    private static final Object NONE = new Object();

    private static Object[] filled(int size) {
        Object[] array = new Object[size];
        Arrays.fill(array, SnapshotJournal.NONE);
        return array;
    }

    private Object[] snapshots = SnapshotJournal.filled(4);

    protected SnapshotJournal() {
    }

    protected abstract S createSnapshot();

    protected abstract void revertToSnapshot(S snapshot);

    /**
     * 根事务提交后调用一次, 参数是这次事务开始前的状态,.在这里做标脏、排视觉刷新这类轻量副作用, 不要做重活,.
     * 默认什么也不做,.
     */
    protected void onRootCommit(S originalState) {
    }

    /**
     * 每次登记快照前调用, 用来校验线程或 Folia 区域归属；不满足时抛 {@link FluidAccessException},.默认什么也不做,.
     *
     * @throws FluidAccessException 当前线程不允许访问这个参与者
     */
    protected void checkAccess() {
    }

    /**
     * 修改状态之前必须先调用,.同一层事务里重复调用没有额外开销,.
     *
     * @throws FluidAccessException  线程或区域不对
     * @throws IllegalStateException 事务已关闭或不是最内层
     */
    public final void updateSnapshots(TransactionContext transaction) {
        Transaction tx = (Transaction) Objects.requireNonNull(transaction, "transaction");
        tx.validateCurrent();
        this.checkAccess();
        int depth = tx.depth;
        if (depth >= this.snapshots.length) {
            Object[] grown = SnapshotJournal.filled(Math.max(depth + 1, this.snapshots.length * 2));
            System.arraycopy(this.snapshots, 0, grown, 0, this.snapshots.length);
            this.snapshots = grown;
        }
        if (this.snapshots[depth] == SnapshotJournal.NONE) {
            this.snapshots[depth] = this.createSnapshot();
            tx.journals.add(this);
        }
    }

    @SuppressWarnings("unchecked")
    final void onClose(Transaction transaction, boolean commit) {
        int depth = transaction.depth;
        S snapshot = (S) this.snapshots[depth];
        this.snapshots[depth] = SnapshotJournal.NONE;
        if (!commit) {
            this.revertToSnapshot(snapshot);
        } else if (depth > 0) {
            if (this.snapshots[depth - 1] == SnapshotJournal.NONE) {
                // The parent had not seen this participant yet: our snapshot is the parent's starting state.
                this.snapshots[depth - 1] = snapshot;
                transaction.manager.stack.get(depth - 1).journals.add(this);
            }
        } else {
            transaction.manager.rootActions.add(() -> this.onRootCommit(snapshot));
        }
    }
}
