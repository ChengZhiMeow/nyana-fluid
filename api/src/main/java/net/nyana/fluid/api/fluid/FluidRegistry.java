package net.nyana.fluid.api.fluid;

import net.momirealms.craftengine.core.util.Key;
import net.nyana.fluid.api.Registration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 流体注册表，任何线程都可以读写；代码注册优先于配置。
 * 注销不删除已有流体：已存的数据读成 {@code UNKNOWN} 并锁定，重新注册后恢复。
 */
@ApiStatus.NonExtendable
public interface FluidRegistry {
    Optional<Fluid> get(Key key);

    /**
     * 按 key 取流体，不存在时抛异常。
     *
     * @throws java.util.NoSuchElementException 未注册
     */
    Fluid getOrThrow(Key key);

    boolean contains(Key key);

    /**
     * 当前快照里的全部流体，按 key 排序。
     */
    Collection<Fluid> all();

    /**
     * 当前快照里的全部流体 key。
     */
    Set<Key> keys();

    /**
     * 某个插件通过代码注册的流体。
     */
    Collection<Fluid> byOwner(Plugin owner);

    /**
     * 一次性取到一致的快照：generation、全部流体、全部标签。
     */
    FluidRegistrySnapshot snapshot();

    /**
     * 当前快照的代数，每次变化加 1。
     */
    long generation();

    FluidTags tags();

    /**
     * 注册一种流体。关闭返回的 {@link Registration} 等同于 {@link #unregister(Plugin, Key)}。
     *
     * @throws IllegalStateException 这个 key 已经被另一个插件通过代码注册
     */
    Registration register(Plugin owner, Key key, FluidAttributes attributes);

    /**
     * 更新自己注册的流体的属性。
     */
    boolean update(Plugin owner, Key key, FluidAttributes attributes);

    /**
     * 注销自己注册的流体。已有的流体不会被删除，见类说明。
     */
    boolean unregister(Plugin owner, Key key);

    /**
     * 注销某个插件注册的全部流体。
     */
    int unregisterAll(Plugin owner);

    /**
     * 批量修改：所有操作合并成一次快照、一次事件。操作抛出异常时整批都不生效。
     */
    void batch(Plugin owner, Consumer<Batch> operations);

    /**
     * {@link #batch(Plugin, Consumer)} 里可用的操作。规则和对应的单个方法相同。
     */
    @ApiStatus.NonExtendable
    interface Batch {
        /**
         * 注册一种流体。
         *
         * @throws IllegalStateException 这个 key 已经被另一个插件通过代码注册
         */
        Batch register(Key key, FluidAttributes attributes);

        /**
         * 更新自己注册的流体；不是自己的则忽略。
         */
        Batch update(Key key, FluidAttributes attributes);

        /**
         * 注销自己注册的流体；不是自己的则忽略。
         */
        Batch unregister(Key key);
    }
}
