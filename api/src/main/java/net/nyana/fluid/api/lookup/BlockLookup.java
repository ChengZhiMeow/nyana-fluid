package net.nyana.fluid.api.lookup;

import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.ExistingBlock;
import net.momirealms.craftengine.core.world.World;
import net.nyana.fluid.api.Registration;
import net.nyana.fluid.api.internal.Lookups;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * 按方块查能力。
 *
 * <p>查找顺序，取第一个非 {@code null} 的结果：
 * ① controller 提供者（遍历 CE 组合 controller 的每个 order）→ ② 按方块 id → ③ 按方块标签 → ④ fallback（按注册顺序）。
 * 区块没有加载时直接返回 {@code null}，不会加载区块。必须在方块所在的区域线程上调用。
 */
@ApiStatus.NonExtendable
public interface BlockLookup<A, C> {
    /**
     * 取（不存在则创建）一个查找。同一个 id 永远返回同一个实例。
     *
     * @throws IllegalArgumentException 这个 id 已经以别的类型创建过
     */
    static <A, C> BlockLookup<A, C> get(Key id, Class<A> apiClass, Class<C> contextClass) {
        return Lookups.block(id, apiClass, contextClass);
    }

    Key id();

    Class<A> apiClass();

    Class<C> contextClass();

    @Nullable A find(ExistingBlock block, @Nullable C context);

    @Nullable A find(World world, BlockPos pos, @Nullable C context);

    /**
     * 查找（Bukkit 方块，内部通过 CE 的 {@code BukkitAdaptor} 转换）。
     */
    @Nullable A find(org.bukkit.block.Block block, @Nullable C context);

    /**
     * 按方块 id 注册。原版方块 id（如 {@code minecraft:cauldron}）和 CE 自定义方块 id 都可以。
     */
    Registration registerForBlocks(Plugin owner, BlockProvider<A, C> provider, Key... blockIds);

    Registration registerForTag(Plugin owner, BlockProvider<A, C> provider, Key blockTag);

    /**
     * 按 CE 方块实体 controller 的类型注册（可以是接口）。同一方块上的全部同类 controller 会一起交给
     * {@link ControllerProvider#findAll}。
     */
    <T> Registration registerForController(Plugin owner, Class<T> controllerType, ControllerProvider<T, A, C> provider);

    /**
     * 注册 fallback，在其他提供者都没有结果时按注册顺序询问。
     */
    Registration registerFallback(Plugin owner, BlockProvider<A, C> provider);

    /**
     * 创建一个固定位置的查找，给轮询邻居的机器用。
     */
    BlockLookupCache<A, C> cache(World world, BlockPos pos);
}
