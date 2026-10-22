package net.nyana.fluid.api.recipe;

import net.momirealms.craftengine.core.util.Key;
import net.nyana.fluid.api.Registration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;

import java.util.Optional;

/**
 * 原料种类注册表,.内置种类：{@code nyana-fluid:fluid}、{@code nyana-fluid:tag}、{@code nyana-fluid:any_of}、
 * {@code nyana-fluid:components}、{@code nyana-fluid:empty},.
 */
@ApiStatus.NonExtendable
public interface FluidIngredientRegistry {
    /**
     * 注册一种原料,.
     *
     * @throws IllegalStateException 这个 key 已经注册了别的种类
     */
    Registration register(Plugin owner, FluidIngredientType<?> type);

    /**
     * 按 key 取原料种类,.
     */
    Optional<FluidIngredientType<?>> get(Key key);
}
