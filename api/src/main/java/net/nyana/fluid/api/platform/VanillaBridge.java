package net.nyana.fluid.api.platform;

import net.nyana.fluid.api.item.BucketSpec;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;

/**
 * 原版内容的版本差异：原版流体对应哪些桶 / 瓶。不同 MC 版本的物品 id 可能改名，映射表放在实现里。
 */
@ApiStatus.Internal
public interface VanillaBridge {
    /**
     * 原版的桶型容器：空桶 ↔ 水桶 / 岩浆桶，玻璃瓶 ↔ 水瓶。
     */
    List<BucketSpec> containers();
}
