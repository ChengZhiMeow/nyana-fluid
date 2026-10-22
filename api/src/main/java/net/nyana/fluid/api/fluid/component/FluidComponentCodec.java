package net.nyana.fluid.api.fluid.component;

import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.libraries.nbt.*;
import net.nyana.fluid.api.persist.FluidDataException;

/**
 * 组件值和 NBT {@link Tag} 之间的转换,.
 *
 * <p>实现必须是确定性的：同一个值总是编码成相等的 {@code Tag}, 这样装着相同流体的两件物品才能堆叠,.
 * 解码失败时抛出 {@link FluidDataException}, 不要返回 {@code null},.
 */
public interface FluidComponentCodec<T> {
    /**
     * {@code int}, 编码为 {@code IntTag}；解码时接受任何数字 tag,.
     */
    FluidComponentCodec<Integer> INT = new FluidComponentCodec<>() {
        @Override
        public Tag encode(Integer value) {
            return IntTag.valueOf(value);
        }

        @Override
        public Integer decode(Tag tag) throws FluidDataException {
            if (tag instanceof NumericTag number) return number.getAsInt();
            throw new FluidDataException("Expected a number, got " + tag);
        }
    };
    /**
     * {@code long}, 编码为 {@code LongTag}；解码时接受任何数字 tag,.
     */
    FluidComponentCodec<Long> LONG = new FluidComponentCodec<>() {
        @Override
        public Tag encode(Long value) {
            return LongTag.valueOf(value);
        }

        @Override
        public Long decode(Tag tag) throws FluidDataException {
            if (tag instanceof NumericTag number) return number.getAsLong();
            throw new FluidDataException("Expected a number, got " + tag);
        }
    };
    /**
     * 字符串, 编码为 {@code StringTag},.
     */
    FluidComponentCodec<String> STRING = new FluidComponentCodec<>() {
        @Override
        public Tag encode(String value) {
            return new StringTag(value);
        }

        @Override
        public String decode(Tag tag) throws FluidDataException {
            if (tag instanceof StringTag string) return string.value();
            throw new FluidDataException("Expected a string, got " + tag);
        }
    };
    /**
     * 布尔值, 编码为 {@code ByteTag}（0 或 1）,.
     */
    FluidComponentCodec<Boolean> BOOL = new FluidComponentCodec<>() {
        @Override
        public Tag encode(Boolean value) {
            return ByteTag.valueOf(value);
        }

        @Override
        public Boolean decode(Tag tag) throws FluidDataException {
            if (tag instanceof NumericTag number) return number.getAsByte() != 0;
            throw new FluidDataException("Expected a boolean, got " + tag);
        }
    };
    /**
     * CE {@link Key}, 编码为 {@code "namespace:value"} 字符串,.
     */
    FluidComponentCodec<Key> KEY = new FluidComponentCodec<>() {
        @Override
        public Tag encode(Key value) {
            return new StringTag(value.toString());
        }

        @Override
        public Key decode(Tag tag) throws FluidDataException {
            if (!(tag instanceof StringTag string)) throw new FluidDataException("Expected a key string, got " + tag);
            try {
                return Key.of(string.value());
            } catch (RuntimeException exception) {
                throw new FluidDataException("Invalid key: " + string.value(), exception);
            }
        }
    };
    /**
     * 原样保存一个复合 tag（编码和解码都做深拷贝）,.
     */
    FluidComponentCodec<CompoundTag> COMPOUND = new FluidComponentCodec<>() {
        @Override
        public Tag encode(CompoundTag value) {
            return value.deepClone();
        }

        @Override
        public CompoundTag decode(Tag tag) throws FluidDataException {
            if (tag instanceof CompoundTag compound) return compound.deepClone();
            throw new FluidDataException("Expected a compound, got " + tag);
        }
    };

    /**
     * 把值编码成 tag,.
     */
    Tag encode(T value);

    /**
     * 把 tag 解码成值,.
     *
     * @throws FluidDataException tag 的结构不合法
     */
    T decode(Tag tag) throws FluidDataException;
}
