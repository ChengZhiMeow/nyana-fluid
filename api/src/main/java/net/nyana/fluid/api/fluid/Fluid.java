package net.nyana.fluid.api.fluid;

import net.momirealms.craftengine.core.util.Key;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * 注册表里的一种流体。
 *
 * <p>{@code Fluid} 对象属于某一个注册表快照：CE reload 或代码注册改变注册表之后，会生成新的 {@code Fluid} 对象。
 * 需要长期保存流体身份时请保存 {@link #key()} 或 {@link FluidVariant}，不要缓存 {@code Fluid} 本身。
 */
public final class Fluid {
    private final Key key;
    private final FluidAttributes attributes;
    private final Source source;
    private final @Nullable Plugin owner;
    private final Set<Key> tags;

    /**
     * 只由注册表实现调用。
     */
    @ApiStatus.Internal
    public Fluid(Key key, FluidAttributes attributes, Source source, @Nullable Plugin owner, Set<Key> tags) {
        this.key = Objects.requireNonNull(key, "key");
        this.attributes = Objects.requireNonNull(attributes, "attributes");
        this.source = Objects.requireNonNull(source, "source");
        this.owner = owner;
        this.tags = Set.copyOf(tags);
    }

    /**
     * 流体 key，例如 {@code minecraft:water}。
     */
    public Key key() {
        return this.key;
    }

    public FluidAttributes attributes() {
        return this.attributes;
    }

    /**
     * 不带组件的默认变体。
     */
    public FluidVariant defaultVariant() {
        return FluidVariant.of(this.key);
    }

    /**
     * 这种流体所属的全部标签。
     */
    public Set<Key> tags() {
        return this.tags;
    }

    /**
     * 是否属于某个标签。
     */
    public boolean is(Key tag) {
        return this.tags.contains(tag);
    }

    public Source source() {
        return this.source;
    }

    /**
     * 代码注册时的 owner 插件。
     */
    public Optional<Plugin> owner() {
        return Optional.ofNullable(this.owner);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Fluid fluid && fluid.key.equals(this.key) && fluid.attributes.equals(this.attributes)
                && fluid.source == this.source && fluid.tags.equals(this.tags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.key, this.attributes, this.source, this.tags);
    }

    @Override
    public String toString() {
        return "Fluid[" + this.key + ", " + this.source + "]";
    }

    /**
     * 流体的注册来源。同一个 key 代码和配置都注册了时，代码注册优先。
     */
    public enum Source {
        /**
         * 由插件代码通过 {@link FluidRegistry#register} 注册，CE reload 后保留。
         */
        CODE,
        /**
         * 由 CE 配置 {@code nyana-fluid:fluids} 注册，每次 CE reload 整体替换。
         */
        CONFIG
    }
}
