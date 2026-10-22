package net.nyana.fluid.api.lookup;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.setting.CustomItemSettingType;
import net.momirealms.craftengine.core.util.Key;
import net.nyana.fluid.api.Registration;
import net.nyana.fluid.api.internal.Lookups;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * 按物品查能力,.
 *
 * <p>查找顺序, 取第一个非 {@code null} 的结果：
 * ① 按 CE 物品设置 → ② 按物品 id（{@link Item#id()}, 原版和 CE 物品同一套）→ ③ 按物品标签 → ④ fallback,.
 */
@ApiStatus.NonExtendable
public interface ItemLookup<A, C> {
    /**
     * 取（不存在则创建）一个查找,.同一个 id 永远返回同一个实例,.
     *
     * @throws IllegalArgumentException 这个 id 已经以别的类型创建过
     */
    static <A, C> ItemLookup<A, C> get(Key id, Class<A> apiClass, Class<C> contextClass) {
        return Lookups.item(id, apiClass, contextClass);
    }

    Key id();

    @Nullable A find(Item item, @Nullable C context);

    Registration registerForItems(Plugin owner, ItemProvider<A, C> provider, Key... itemIds);

    /**
     * 按物品标签注册（原版标签或 CE 物品标签）,.
     */
    Registration registerForTag(Plugin owner, ItemProvider<A, C> provider, Key itemTag);

    /**
     * 按 CE 物品设置注册,.
     */
    <S> Registration registerForSetting(Plugin owner, CustomItemSettingType<S> setting, SettingProvider<S, A, C> provider);

    /**
     * 注册 fallback,.
     */
    Registration registerFallback(Plugin owner, ItemProvider<A, C> provider);
}
