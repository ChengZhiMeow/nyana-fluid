package net.nyana.fluid.api.fluid;

import net.momirealms.craftengine.core.util.Key;

import java.util.Objects;

/**
 * 变体 + 数量（mB），用于展示、配置、事件和持久化。存储接口本身只用 {@code (variant, amount)} 两个参数。
 *
 * <p>「空」只有一种表示：数量为 0 时变体一定是 {@link FluidVariant#blank()}，反之亦然。
 * 增加数量用 {@link Math#addExact(long, long)}，溢出直接抛 {@link ArithmeticException}，不会静默变空。
 */
public record FluidStack(FluidVariant variant, long amount) {
    public static final FluidStack EMPTY = new FluidStack(FluidVariant.blank(), 0);

    /**
     * 构造一个堆；数量为 0 时返回 {@link #EMPTY}。
     */
    public static FluidStack of(FluidVariant variant, long amount) {
        return amount == 0 ? FluidStack.EMPTY : new FluidStack(variant, amount);
    }

    /**
     * 用不带组件的变体构造一个堆。
     */
    public static FluidStack of(Key fluid, long amount) {
        return FluidStack.of(FluidVariant.of(fluid), amount);
    }

    /**
     * 规范化构造：数量为 0 时变体归一为空变体。
     *
     * @throws IllegalArgumentException 数量为负，或者空变体配了正数量
     */
    public FluidStack {
        Objects.requireNonNull(variant, "variant");
        if (amount < 0) throw new IllegalArgumentException("Fluid amount must not be negative: " + amount);
        if (amount == 0) variant = FluidVariant.blank();
        else if (variant.isBlank()) throw new IllegalArgumentException("A non-empty stack needs a fluid");
    }

    public boolean isEmpty() {
        return this.amount == 0;
    }

    /**
     * 同一变体，换一个数量。
     *
     * @throws IllegalStateException 对空堆设置正数量
     */
    public FluidStack withAmount(long newAmount) {
        if (this.isEmpty() && newAmount > 0) throw new IllegalStateException("An empty stack has no fluid to resize");
        return FluidStack.of(this.variant, newAmount);
    }

    /**
     * 增加数量，溢出抛异常。
     *
     * @throws ArithmeticException      溢出
     * @throws IllegalStateException    对空堆增加
     * @throws IllegalArgumentException 增量为负
     */
    public FluidStack grow(long delta) {
        if (delta < 0) throw new IllegalArgumentException("Delta must not be negative");
        if (delta == 0) return this;
        if (this.isEmpty()) throw new IllegalStateException("An empty stack has no fluid to grow");
        return FluidStack.of(this.variant, Math.addExact(this.amount, delta));
    }

    /**
     * 减少数量，最多减到 0。
     *
     * @throws IllegalArgumentException 减量为负
     */
    public FluidStack shrink(long delta) {
        if (delta < 0) throw new IllegalArgumentException("Delta must not be negative");
        return delta == 0 ? this : FluidStack.of(this.variant, this.amount - Math.min(this.amount, delta));
    }
}
