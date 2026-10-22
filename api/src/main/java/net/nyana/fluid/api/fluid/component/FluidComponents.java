package net.nyana.fluid.api.fluid.component;

import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.libraries.nbt.Tag;
import net.nyana.fluid.api.persist.FluidDataException;

import java.util.*;

/**
 * 一组组件，不可变，按 key 的字符串顺序排列，保证编码结果是确定的。
 *
 * <p>内部保存的是编码后的 NBT {@link Tag}（深拷贝），所以即使某个组件类型当前没有注册，
 * 也可以通过 {@link #raw(Key)} 读到原始值。
 */
public final class FluidComponents {
    public static final FluidComponents EMPTY = new FluidComponents(new TreeMap<>());

    /**
     * 新建一个 builder。
     */
    public static Builder builder() {
        return new Builder(new TreeMap<>());
    }

    private final TreeMap<String, Entry> entries;
    private final int hash;

    private FluidComponents(TreeMap<String, Entry> entries) {
        this.entries = entries;
        int h = 0;
        for (Entry entry : entries.values()) h = 31 * h + entry.hashCode();
        this.hash = h;
    }

    /**
     * 以当前内容为起点新建一个 builder。
     */
    public Builder toBuilder() {
        return new Builder(new TreeMap<>(this.entries));
    }

    /**
     * 全部组件 key，按字符串顺序。
     */
    public Set<Key> keys() {
        Set<Key> keys = new LinkedHashSet<>();
        for (Entry entry : this.entries.values()) keys.add(entry.key);
        return Collections.unmodifiableSet(keys);
    }

    /**
     * 是否含有某个组件。
     */
    public boolean has(Key key) {
        return this.entries.containsKey(key.toString());
    }

    /**
     * 读取一个组件并解码。
     *
     * @throws IllegalStateException 保存的 tag 无法用这个类型的 codec 解码
     */
    public <T> Optional<T> get(FluidComponentType<T> type) {
        Entry entry = this.entries.get(type.key().toString());
        if (entry == null) return Optional.empty();
        try {
            return Optional.of(type.codec().decode(entry.tag.deepClone()));
        } catch (FluidDataException exception) {
            throw new IllegalStateException("Component " + type.key() + " cannot be decoded: " + exception.getMessage(), exception);
        }
    }

    /**
     * 读取一个组件的原始 tag（深拷贝）。
     */
    public Optional<Tag> raw(Key key) {
        Entry entry = this.entries.get(key.toString());
        return entry == null ? Optional.empty() : Optional.of(entry.tag.deepClone());
    }

    /**
     * 返回加上（或替换）一个组件后的新集合。
     */
    public <T> FluidComponents with(FluidComponentType<T> type, T value) {
        return this.toBuilder().put(type, value).build();
    }

    /**
     * 返回去掉一个组件后的新集合。
     */
    public FluidComponents without(Key key) {
        if (!this.has(key)) return this;
        return this.toBuilder().remove(key).build();
    }

    public int size() {
        return this.entries.size();
    }

    public boolean isEmpty() {
        return this.entries.isEmpty();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        return other instanceof FluidComponents components && components.hash == this.hash && components.entries.equals(this.entries);
    }

    @Override
    public int hashCode() {
        return this.hash;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("{");
        for (Map.Entry<String, Entry> entry : this.entries.entrySet()) {
            if (builder.length() > 1) builder.append(", ");
            builder.append(entry.getKey()).append('=').append(entry.getValue().tag);
        }
        return builder.append('}').toString();
    }

    private record Entry(Key key, Tag tag) {
    }

    public static final class Builder {
        private final TreeMap<String, Entry> entries;

        private Builder(TreeMap<String, Entry> entries) {
            this.entries = entries;
        }

        /**
         * 放入一个组件，已有同 key 的组件会被替换。
         */
        public <T> Builder put(FluidComponentType<T> type, T value) {
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(value, "value");
            Tag tag = Objects.requireNonNull(type.codec().encode(value), "Component codec returned null");
            this.entries.put(type.key().toString(), new Entry(type.key(), tag.deepClone()));
            return this;
        }

        /**
         * 直接放入一个原始 tag。读取未注册的组件、做数据迁移时使用。
         */
        public Builder putRaw(Key key, Tag tag) {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(tag, "tag");
            this.entries.put(key.toString(), new Entry(key, tag.deepClone()));
            return this;
        }

        public Builder remove(Key key) {
            this.entries.remove(key.toString());
            return this;
        }

        public FluidComponents build() {
            return this.entries.isEmpty() ? FluidComponents.EMPTY : new FluidComponents(new TreeMap<>(this.entries));
        }
    }
}
