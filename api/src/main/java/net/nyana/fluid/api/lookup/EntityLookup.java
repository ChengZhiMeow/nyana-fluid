package net.nyana.fluid.api.lookup;

import net.momirealms.craftengine.core.entity.Entity;
import net.momirealms.craftengine.core.util.Key;
import net.nyana.fluid.api.Registration;
import net.nyana.fluid.api.internal.Lookups;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * 按实体查能力,.查找顺序：① 按实体类型 → ② fallback,.必须在实体所在的区域线程上调用,.
 */
@ApiStatus.NonExtendable
public interface EntityLookup<A, C> {
    /**
     * 取（不存在则创建）一个查找,.
     *
     * @throws IllegalArgumentException 这个 id 已经以别的类型创建过
     */
    static <A, C> EntityLookup<A, C> get(Key id, Class<A> apiClass, Class<C> contextClass) {
        return Lookups.entity(id, apiClass, contextClass);
    }

    Key id();

    @Nullable A find(Entity entity, @Nullable C context);

    /**
     * 查找（Bukkit 实体, 内部通过 CE 的 {@code BukkitAdaptor} 转换）,.
     */
    @Nullable A find(org.bukkit.entity.Entity entity, @Nullable C context);

    /**
     * 按实体类型注册, 例如 {@code minecraft:cow},.
     */
    Registration registerForTypes(Plugin owner, EntityProvider<A, C> provider, Key... entityTypes);

    /**
     * 注册 fallback,.
     */
    Registration registerFallback(Plugin owner, EntityProvider<A, C> provider);
}
