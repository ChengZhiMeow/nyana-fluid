package net.nyana.fluid.api.platform;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.UUID;

/**
 * 只发包、不存盘的物品展示实体。返回的都是可以直接发给玩家的包。
 */
@ApiStatus.Internal
public interface DisplayBridge {
    int nextEntityId();

    Object spawnItemDisplay(int id, UUID uuid, double x, double y, double z);

    Object remove(int... ids);

    /**
     * 全部数据；{@code interpolationDuration} 为 0 时立即生效。
     */
    Object itemDisplayData(int id, ItemDisplayState state, int interpolationDelay, int interpolationDuration);

    /**
     * 一个物品展示实体要显示的内容。{@code brightness} 为 -1 时不覆盖亮度。
     */
    record ItemDisplayState(@Nullable ItemStack item, Vector3f translation, Vector3f scale, Quaternionf leftRotation,
                            int brightness, float viewRange) {
    }
}
