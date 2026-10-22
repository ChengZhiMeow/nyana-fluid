package net.nyana.fluid.api.lookup;

import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.util.Key;
import net.nyana.fluid.api.Registration;
import net.nyana.fluid.api.internal.Lookups;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * 按 CE 家具查能力（Fabric、NeoForge 都没有这一项）,.查找顺序：① 按家具 id → ② fallback,.
 */
@ApiStatus.NonExtendable
public interface FurnitureLookup<A, C> {
    /**
     * 取（不存在则创建）一个查找,.
     *
     * @throws IllegalArgumentException 这个 id 已经以别的类型创建过
     */
    static <A, C> FurnitureLookup<A, C> get(Key id, Class<A> apiClass, Class<C> contextClass) {
        return Lookups.furniture(id, apiClass, contextClass);
    }

    Key id();

    @Nullable A find(Furniture furniture, @Nullable C context);

    Registration registerForFurniture(Plugin owner, FurnitureProvider<A, C> provider, Key... furnitureIds);

    /**
     * 注册 fallback,.
     */
    Registration registerFallback(Plugin owner, FurnitureProvider<A, C> provider);
}
