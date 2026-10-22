package net.nyana.fluid.api.transaction;

/**
 * 交给存储实现的事务上下文,.它只暴露嵌套深度, 存储实现无法提交或关闭事务, 只能通过
 * {@link SnapshotJournal#updateSnapshots(TransactionContext)} 参与,.
 *
 * <p>这里的 {@code sealed} 只用来禁止 addon 自己实现这个接口, 不用于分类判断,.
 */
public sealed interface TransactionContext permits Transaction {
    /**
     * 嵌套深度：根事务为 0, 每嵌套一层加 1,.
     */
    int depth();
}
