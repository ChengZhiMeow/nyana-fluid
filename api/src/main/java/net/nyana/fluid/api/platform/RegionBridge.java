package net.nyana.fluid.api.platform;

import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.ApiStatus;

/**
 * 区域归属校验,.Paper 上等价于「是不是主线程」, Folia 上是「当前线程是否拥有这个区块 / 实体所在的区域」,.
 * 实现不得缓存区域令牌：Folia 合并区域后旧令牌会失效,.
 */
@ApiStatus.Internal
public interface RegionBridge {
    /**
     * 当前线程是否拥有某个区块,.
     */
    boolean ownsChunk(World world, int chunkX, int chunkZ);

    /**
     * 当前线程是否拥有某个实体,.
     */
    boolean ownsEntity(Entity entity);
}
