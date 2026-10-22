package net.nyana.fluid.api.fluid;

import net.momirealms.craftengine.core.util.Key;
import net.nyana.fluid.api.Registration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;

import java.util.Collection;
import java.util.Set;

/**
 * 流体标签。标签可以由代码注册，也可以由 CE 配置 {@code nyana-fluid:fluid_tags} 声明，两者取并集。
 * 标签成员可以引用尚未注册的流体。
 */
@ApiStatus.NonExtendable
public interface FluidTags {
    /**
     * 某种流体是否属于某个标签。
     */
    boolean is(Key fluid, Key tag);

    Set<Key> members(Key tag);

    /**
     * 某种流体所属的全部标签。
     */
    Set<Key> tagsOf(Key fluid);

    /**
     * 往标签里加成员。关闭返回的 {@link Registration} 会撤销这次添加。
     */
    Registration register(Plugin owner, Key tag, Collection<Key> fluids);
}
