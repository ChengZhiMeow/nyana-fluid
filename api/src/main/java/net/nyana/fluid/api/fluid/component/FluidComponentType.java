package net.nyana.fluid.api.fluid.component;

import net.momirealms.craftengine.core.util.Key;

import java.util.Objects;

/**
 * 一种组件：唯一标识加上它的 codec,.两个组件类型只要 {@link #key()} 相同就视为同一种,.
 */
public final class FluidComponentType<T> {
    /**
     * 创建组件类型,.创建本身不会注册它, 持久化前要调用 {@link FluidComponentRegistry#register},.
     */
    public static <T> FluidComponentType<T> of(Key key, FluidComponentCodec<T> codec) {
        return new FluidComponentType<>(key, codec);
    }

    private final Key key;
    private final FluidComponentCodec<T> codec;

    private FluidComponentType(Key key, FluidComponentCodec<T> codec) {
        this.key = Objects.requireNonNull(key, "key");
        this.codec = Objects.requireNonNull(codec, "codec");
    }

    public Key key() {
        return this.key;
    }

    public FluidComponentCodec<T> codec() {
        return this.codec;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof FluidComponentType<?> type && type.key.equals(this.key);
    }

    @Override
    public int hashCode() {
        return this.key.hashCode();
    }

    @Override
    public String toString() {
        return "FluidComponentType[" + this.key + "]";
    }
}
