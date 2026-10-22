package net.nyana.fluid.api.transaction;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 线程绑定、可嵌套的事务。只有最内层可以操作；根事务提交后才执行副作用（{@link SnapshotJournal#onRootCommit(Object)}）。
 */
@ApiStatus.NonExtendable
public final class Transaction implements TransactionContext, AutoCloseable {
    private static final ThreadLocal<Manager> MANAGERS = ThreadLocal.withInitial(Manager::new);

    /**
     * 打开根事务。
     *
     * @throws IllegalStateException 当前线程已经有打开的事务（请改用 {@link #open(TransactionContext)} 嵌套）
     */
    public static Transaction openRoot() {
        Manager manager = Transaction.MANAGERS.get();
        if (!manager.stack.isEmpty()) {
            throw new IllegalStateException("A transaction is already open on this thread; use Transaction.open(parent) to nest");
        }
        return manager.push();
    }

    /**
     * 在 {@code parent} 里嵌套打开一个事务；{@code parent} 为 {@code null} 时打开根事务。
     *
     * @throws FluidAccessException  在别的线程上使用 {@code parent}
     * @throws IllegalStateException {@code parent} 已关闭，或者不是最内层的事务
     */
    public static Transaction open(@Nullable TransactionContext parent) {
        if (parent == null) return Transaction.openRoot();
        Transaction transaction = (Transaction) parent;
        transaction.validateCurrent();
        return transaction.manager.push();
    }

    /**
     * 当前线程是否有打开的事务。
     */
    public static boolean isOpen() {
        return !Transaction.MANAGERS.get().stack.isEmpty();
    }

    /**
     * 当前线程最内层的事务。
     */
    public static @Nullable TransactionContext current() {
        List<Transaction> stack = Transaction.MANAGERS.get().stack;
        return stack.isEmpty() ? null : stack.get(stack.size() - 1);
    }

    /**
     * 当前线程的事务生命周期阶段。
     */
    public static Lifecycle lifecycle() {
        return Transaction.MANAGERS.get().lifecycle;
    }

    final Manager manager;
    final int depth;
    final List<SnapshotJournal<?>> journals = new ArrayList<>();
    private boolean open = true;

    private Transaction(Manager manager, int depth) {
        this.manager = manager;
        this.depth = depth;
    }

    /**
     * 提交并关闭这个事务。嵌套事务的修改并入父事务；根事务提交后执行各参与者的根提交回调。
     *
     * @throws FluidAccessException  在别的线程上调用
     * @throws IllegalStateException 事务已关闭，或者还有未关闭的内层事务
     * @throws RuntimeException      根提交回调抛出的异常；此时状态<b>已经</b>提交
     */
    public void commit() {
        this.validateCurrent();
        this.finish(true);
    }

    /**
     * 关闭这个事务；如果还没提交，就回滚它做过的全部修改。重复调用没有效果。
     *
     * @throws FluidAccessException  在别的线程上调用
     * @throws IllegalStateException 还有未关闭的内层事务
     */
    @Override
    public void close() {
        if (!this.open) return;
        this.validateCurrent();
        this.finish(false);
    }

    @Override
    public int depth() {
        return this.depth;
    }

    /**
     * 这个事务是否仍然打开。
     */
    public boolean isOpenNow() {
        return this.open;
    }

    void validateCurrent() {
        if (Thread.currentThread() != this.manager.thread) {
            throw new FluidAccessException("Transaction used from thread " + Thread.currentThread().getName()
                    + " but it belongs to " + this.manager.thread.getName());
        }
        if (!this.open) throw new IllegalStateException("Transaction is closed");
        if (this.manager.top() != this) throw new IllegalStateException("Close the nested transaction first");
    }

    private void finish(boolean commit) {
        Manager manager = this.manager;
        manager.lifecycle = Lifecycle.CLOSING;
        RuntimeException failure = null;
        for (int i = this.journals.size() - 1; i >= 0; i--) {
            try {
                this.journals.get(i).onClose(this, commit);
            } catch (RuntimeException exception) {
                if (failure == null) failure = exception;
                else failure.addSuppressed(exception);
            }
        }
        this.journals.clear();
        this.open = false;
        manager.stack.remove(manager.stack.size() - 1);
        if (this.depth == 0) {
            manager.lifecycle = Lifecycle.OUTER_CLOSING;
            List<Runnable> actions = new ArrayList<>(manager.rootActions);
            manager.rootActions.clear();
            try {
                if (commit) {
                    for (Runnable action : actions) {
                        try {
                            action.run();
                        } catch (RuntimeException exception) {
                            if (failure == null) failure = exception;
                            else failure.addSuppressed(exception);
                        }
                    }
                }
            } finally {
                manager.lifecycle = Lifecycle.NONE;
            }
        } else {
            manager.lifecycle = Lifecycle.OPEN;
        }
        if (failure != null) throw failure;
    }

    @Override
    public String toString() {
        return "Transaction[depth=" + this.depth + ", open=" + this.open + "]";
    }

    /**
     * 当前线程的事务生命周期阶段。switch 时必须带 default 分支。
     */
    public enum Lifecycle {
        NONE,
        OPEN,
        /**
         * 某个事务正在提交或回滚，参与者正在处理快照。
         */
        CLOSING,
        /**
         * 根事务已关闭，正在执行根提交回调。
         */
        OUTER_CLOSING
    }

    static final class Manager {
        final Thread thread = Thread.currentThread();
        final List<Transaction> stack = new ArrayList<>();
        final List<Runnable> rootActions = new ArrayList<>();
        Lifecycle lifecycle = Lifecycle.NONE;

        Transaction push() {
            if (this.lifecycle == Lifecycle.CLOSING)
                throw new IllegalStateException("Cannot open a transaction while one is closing");
            Transaction transaction = new Transaction(this, this.stack.size());
            this.stack.add(transaction);
            this.lifecycle = Lifecycle.OPEN;
            return transaction;
        }

        @Nullable Transaction top() {
            return this.stack.isEmpty() ? null : this.stack.get(this.stack.size() - 1);
        }
    }
}
