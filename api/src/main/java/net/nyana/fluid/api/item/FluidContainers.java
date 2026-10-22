package net.nyana.fluid.api.item;

import net.momirealms.craftengine.core.item.Item;
import net.nyana.fluid.api.Registration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;
import java.util.Optional;

/**
 * 物品容器的注册,.桶型容器注册后会自动出现在 {@link net.nyana.fluid.api.lookup.FluidLookups#ITEM} 里；
 * 数据型容器通过 CE 物品设置 {@code nyana-fluid:container} 声明, 同样由插件自动接进查找,.
 */
@ApiStatus.NonExtendable
public interface FluidContainers {
    /**
     * 注册桶型容器,.同一个空物品可以注册多种流体（空桶 → 水桶 / 岩浆桶 / 牛奶桶）,.
     */
    Registration registerBucket(Plugin owner, BucketSpec spec);

    /**
     * 当前全部桶型容器,.
     */
    List<BucketSpec> buckets();

    /**
     * 物品的数据型容器规格,.
     */
    Optional<ContainerSpec> containerSpec(Item item);
}
