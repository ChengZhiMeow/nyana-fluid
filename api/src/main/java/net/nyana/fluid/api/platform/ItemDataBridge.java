package net.nyana.fluid.api.platform;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.libraries.nbt.Tag;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * {@link ItemDataBridge#direct()}: delegates straight to CE's {@link Item} tag accessors.
 */
enum DirectItemData implements ItemDataBridge {
    INSTANCE;

    @Override
    public @Nullable Tag read(Item item, String key) {
        Tag tag = item.getSparrowTag(key);
        return tag == null ? null : tag.deepClone();
    }

    @Override
    public void write(Item item, String key, @Nullable Tag value) {
        if (value == null) item.removeTag(key);
        else item.setSparrowTag(value.deepClone(), key);
    }
}

/**
 * 物品自定义数据的读写。1.20.5+ 的 data component 和旧版 NBT 的差异已经由 CE 的 {@link Item} 抹平，
 * 这里是兜底的位置。
 */
@ApiStatus.Internal
public interface ItemDataBridge {
    /**
     * 直接使用 CE {@link Item} 方法的实现，没有安装插件时使用。
     */
    static ItemDataBridge direct() {
        return DirectItemData.INSTANCE;
    }

    /**
     * 读取 {@code custom_data} 下某个键的数据。
     */
    @Nullable Tag read(Item item, String key);

    /**
     * 写入 {@code custom_data} 下某个键的数据，原地修改物品。
     */
    void write(Item item, String key, @Nullable Tag value);
}
