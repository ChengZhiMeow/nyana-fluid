package net.nyana.fluid.api.fluid.component;

import net.momirealms.craftengine.core.util.Key;
import net.nyana.fluid.api.Registration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;

import java.util.Collection;
import java.util.Optional;

/**
 * 组件类型注册表。持久化数据里出现未注册的组件时，数据会读成 {@code UNKNOWN} 并锁定，
 * 所以带组件的流体在写入之前，组件类型必须先注册。
 */
@ApiStatus.NonExtendable
public interface FluidComponentRegistry {
    /**
     * 注册一种组件类型。
     *
     * @throws IllegalStateException 同一个 key 已经注册了另一个类型
     */
    Registration register(Plugin owner, FluidComponentType<?> type);

    /**
     * 按 key 取组件类型。
     */
    Optional<FluidComponentType<?>> get(Key key);

    /**
     * 全部已注册的组件类型。
     */
    Collection<FluidComponentType<?>> all();
}
