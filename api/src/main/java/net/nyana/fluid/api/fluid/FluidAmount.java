package net.nyana.fluid.api.fluid;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;

/**
 * 数量单位常量的<b>唯一</b>定义处，以及数量的格式化与解析。
 *
 * <p>三个常量都是编译期常量，javac 会把值内联进 addon，所以它们的值永远不会改变，属于契约的一部分。
 * 其他类不会再重复定义这些常量。
 */
public final class FluidAmount {
    /**
     * 1 毫桶（mB），最小单位。
     */
    public static final long MILLIBUCKET = 1;
    /**
     * 1 瓶 = 250 mB。注意原版炼药锅一层是 1/3 桶，和瓶不对应。
     */
    public static final long BOTTLE = 250;
    /**
     * 1 桶 = 1000 mB。
     */
    public static final long BUCKET = 1000;

    /**
     * 格式化一个数量：小于 1 桶显示 {@code 250 mB}，否则显示 {@code 1.25 B}。
     */
    public static String format(long amount) {
        return Math.abs(amount) < FluidAmount.BUCKET ? amount + " mB" : FluidAmount.buckets(amount) + " B";
    }

    /**
     * 格式化「数量 / 容量」：容量不小于 1 桶时两边都用桶，例如 {@code 1.25 / 16 B}；否则都用 mB。
     */
    public static String format(long amount, long capacity) {
        if (Math.abs(capacity) < FluidAmount.BUCKET) return amount + " / " + capacity + " mB";
        return FluidAmount.buckets(amount) + " / " + FluidAmount.buckets(capacity) + " B";
    }

    private static String buckets(long amount) {
        return BigDecimal.valueOf(amount).movePointLeft(3).stripTrailingZeros().toPlainString();
    }

    /**
     * 解析 {@code "1.5B"}、{@code "250mb"}、{@code "250"} 这类文本；不带单位时按 mB 处理。不区分大小写，允许空格。
     *
     * @throws IllegalArgumentException 不是合法的数量，或者换算后不是整数 mB
     */
    public static long parse(String text) {
        Objects.requireNonNull(text, "text");
        String value = text.trim().toLowerCase(Locale.ROOT).replace(" ", "");
        BigDecimal number;
        try {
            if (value.endsWith("mb")) number = new BigDecimal(value.substring(0, value.length() - 2));
            else if (value.endsWith("b"))
                number = new BigDecimal(value.substring(0, value.length() - 1)).movePointRight(3);
            else number = new BigDecimal(value);
            return number.longValueExact();
        } catch (ArithmeticException | NumberFormatException exception) {
            throw new IllegalArgumentException("Not a fluid amount: " + text, exception);
        }
    }

    private FluidAmount() {
    }
}
