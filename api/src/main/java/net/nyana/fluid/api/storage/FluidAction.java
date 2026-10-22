package net.nyana.fluid.api.storage;

/**
 * 便捷方法 {@code fill} / {@code drain} / {@code tryMove} 用的执行方式。存储实现本身不区分这两种方式：
 * 模拟就是开事务但不提交。
 */
public enum FluidAction {
    /**
     * 只计算结果，不改变任何状态。
     */
    SIMULATE,
    EXECUTE;

    public boolean execute() {
        return this == FluidAction.EXECUTE;
    }
}
