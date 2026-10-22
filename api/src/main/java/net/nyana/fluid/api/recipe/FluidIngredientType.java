package net.nyana.fluid.api.recipe;

import net.momirealms.craftengine.core.util.Key;

import java.util.Map;
import java.util.Objects;

/**
 * 一种原料：唯一标识加上配置解析器,.注册后, 配置里的 {@code {type: <key>, ...}} 会交给它解析,.
 */
public final class FluidIngredientType<T extends FluidIngredient> {
    public static <T extends FluidIngredient> FluidIngredientType<T> of(Key key, Parser<T> parser) {
        return new FluidIngredientType<>(key, parser);
    }

    private final Key key;
    private final Parser<T> parser;

    private FluidIngredientType(Key key, Parser<T> parser) {
        this.key = Objects.requireNonNull(key, "key");
        this.parser = Objects.requireNonNull(parser, "parser");
    }

    public Key key() {
        return this.key;
    }

    public Parser<T> parser() {
        return this.parser;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof FluidIngredientType<?> type && type.key.equals(this.key);
    }

    @Override
    public int hashCode() {
        return this.key.hashCode();
    }

    @Override
    public String toString() {
        return "FluidIngredientType[" + this.key + "]";
    }

    @FunctionalInterface
    public interface Parser<T extends FluidIngredient> {
        /**
         * 解析一个 {@code {type: ..., ...}} 映射,.
         *
         * @throws IllegalArgumentException 配置不合法
         */
        T parse(Map<String, Object> config);
    }
}
