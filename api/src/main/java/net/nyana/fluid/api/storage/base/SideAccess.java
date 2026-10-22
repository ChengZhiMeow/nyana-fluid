package net.nyana.fluid.api.storage.base;

import java.util.Locale;

/**
 * 一个面允许的方向,.这是封闭的集合, 所以用枚举,.
 */
public enum SideAccess {
    /**
     * 这个面不暴露存储,.
     */
    NONE,
    /**
     * 只允许从这个面插入,.
     */
    INPUT,
    /**
     * 只允许从这个面抽取,.
     */
    OUTPUT,
    BOTH;

    /**
     * 解析配置里的写法：{@code none} / {@code input} / {@code output} / {@code both}, 不区分大小写,.
     *
     * @throws IllegalArgumentException 不认识的写法
     */
    public static SideAccess parse(String text) {
        try {
            return SideAccess.valueOf(text.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Side access must be none, input, output or both, got: " + text, exception);
        }
    }

    public boolean allowsInsertion() {
        return this == SideAccess.INPUT || this == SideAccess.BOTH;
    }

    public boolean allowsExtraction() {
        return this == SideAccess.OUTPUT || this == SideAccess.BOTH;
    }
}
