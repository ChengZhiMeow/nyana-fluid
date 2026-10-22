package net.nyana.fluid.api.lookup;

import net.momirealms.craftengine.core.item.Item;
import org.jetbrains.annotations.Nullable;

/**
 * 按 CE 物品设置（{@code CustomItemSettingType}）提供能力：物品定义里配置了这个设置时才会被询问,.
 */
@FunctionalInterface
public interface SettingProvider<S, A, C> {
    @Nullable A find(S setting, Item item, @Nullable C context);
}
