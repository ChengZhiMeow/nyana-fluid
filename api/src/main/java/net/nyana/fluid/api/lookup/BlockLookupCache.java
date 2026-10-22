package net.nyana.fluid.api.lookup;

import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * 固定位置上的查找, 给每 tick 轮询邻居的机器和管道用,.区块没有加载时返回 {@code null}, 不会加载区块,.
 *
 * <p>结果按上下文缓存：只要这个位置的方块状态、方块实体和查找的注册都没变, 就直接返回上次的结果（{@code null} 也会缓存）,
 * 不再逐个询问提供者,.每次调用仍会读一下这个位置的方块来判断有没有变化,.一个缓存只给一台机器用, 并且只在它所在的线程上调用,
 * 不是线程安全的,.
 */
public interface BlockLookupCache<A, C> {
    @Nullable A find(@Nullable C context);

    World world();

    BlockPos pos();
}
