package net.nyana.fluid.api.block.tick;

/**
 * Shared instance for {@link TickGate#alwaysAwake()}.
 */
enum AlwaysAwake implements TickGate {
    INSTANCE;

    @Override
    public boolean awake() {
        return true;
    }

    @Override
    public void wake() {
    }

    @Override
    public void sleep() {
    }
}

/**
 * tick 门控：让方块实体的业务逻辑在没事可做时休眠，有事时再唤醒。
 *
 * <p>CE 26.9 上是软门控（CE 每 tick 仍调用一次入口，但只读一个布尔值）；CE 26.10+ 上是原生休眠（CE 不再调用）。
 * 两种实现的 API 完全相同。所有方法只能在方块所在的区域线程上调用。
 */
public interface TickGate {
    /**
     * 一个永远醒着、{@code sleep()} 不起作用的门控，用于不需要休眠的方块。
     */
    static TickGate alwaysAwake() {
        return AlwaysAwake.INSTANCE;
    }

    boolean awake();

    /**
     * 唤醒。下一 tick 开始执行业务逻辑。已经醒着时没有效果。
     */
    void wake();

    /**
     * 休眠，直到被 {@link #wake()} 唤醒。
     */
    void sleep();
}
