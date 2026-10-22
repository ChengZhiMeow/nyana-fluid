package net.nyana.fluid.api.fluid;

import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.ApiStatus;

import java.util.*;

/**
 * 注册表在某一代的不可变快照,.遍历期间注册表发生变化也不会影响快照,.
 */
public final class FluidRegistrySnapshot {
    private final long generation;
    private final Map<Key, Fluid> fluids;
    private final Map<Key, Set<Key>> tags;

    /**
     * 只由注册表实现调用,.
     */
    @ApiStatus.Internal
    public FluidRegistrySnapshot(long generation, Map<Key, Fluid> fluids, Map<Key, Set<Key>> tags) {
        this.generation = generation;
        this.fluids = Collections.unmodifiableMap(new LinkedHashMap<>(fluids));
        Map<Key, Set<Key>> copy = new LinkedHashMap<>();
        tags.forEach((tag, members) -> copy.put(tag, Set.copyOf(members)));
        this.tags = Collections.unmodifiableMap(copy);
    }

    public long generation() {
        return this.generation;
    }

    /**
     * 全部流体, 按 key 排序,.
     */
    public Map<Key, Fluid> fluids() {
        return this.fluids;
    }

    /**
     * 全部标签：标签 key → 成员流体 key,.只包含至少有一个成员的标签,.
     */
    public Map<Key, Set<Key>> tags() {
        return this.tags;
    }

    public Optional<Fluid> get(Key key) {
        return Optional.ofNullable(this.fluids.get(key));
    }

    @Override
    public String toString() {
        return "FluidRegistrySnapshot[generation=" + this.generation + ", fluids=" + this.fluids.size() + ", tags=" + this.tags.size() + "]";
    }
}
