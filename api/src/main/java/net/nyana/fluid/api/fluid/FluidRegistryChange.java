package net.nyana.fluid.api.fluid;

import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;
import java.util.Set;

/**
 * 注册表的一次变化：新增、更新、移除了哪些流体，以及原因。随
 * {@link net.nyana.fluid.api.event.FluidRegistryChangeEvent} 派发。
 */
public final class FluidRegistryChange {
    private final long generation;
    private final Set<Key> added;
    private final Set<Key> removed;
    private final Set<Key> updated;
    private final Cause cause;

    /**
     * 只由注册表实现调用。
     */
    @ApiStatus.Internal
    public FluidRegistryChange(long generation, Set<Key> added, Set<Key> removed, Set<Key> updated, Cause cause) {
        this.generation = generation;
        this.added = Set.copyOf(added);
        this.removed = Set.copyOf(removed);
        this.updated = Set.copyOf(updated);
        this.cause = Objects.requireNonNull(cause, "cause");
    }

    public long generation() {
        return this.generation;
    }

    public Set<Key> added() {
        return this.added;
    }

    public Set<Key> removed() {
        return this.removed;
    }

    /**
     * 属性、来源或标签有变化的流体。
     */
    public Set<Key> updated() {
        return this.updated;
    }

    public Cause cause() {
        return this.cause;
    }

    public boolean isEmpty() {
        return this.added.isEmpty() && this.removed.isEmpty() && this.updated.isEmpty();
    }

    @Override
    public String toString() {
        return "FluidRegistryChange[generation=" + this.generation + ", cause=" + this.cause + ", added=" + this.added
                + ", removed=" + this.removed + ", updated=" + this.updated + "]";
    }

    /**
     * 变化原因。switch 时必须带 default 分支，以后可能增加新的原因。
     */
    public enum Cause {
        /**
         * 插件通过 API 注册、更新、注销。
         */
        API,
        /**
         * CE 加载或重载配置。
         */
        CONFIG_RELOAD
    }
}
