package net.nyana.fluid.api.lookup;

import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.Key;
import net.nyana.fluid.api.item.ItemAccess;
import net.nyana.fluid.api.storage.FluidStorage;

/**
 * 流体用的预定义查找实例。
 *
 * <p>NyanaFluid 插件给 {@link #BLOCK} 预先注册了一个 controller 提供者：任何实现了
 * {@link net.nyana.fluid.api.block.FluidBlockEntity} 的 CE controller 都能被自动查到；同一方块上有多个时，
 * 按面合并成一个 {@link net.nyana.fluid.api.storage.base.CombinedFluidStorage}。
 */
public final class FluidLookups {
    /**
     * 方块；上下文是面，{@code null} 表示内部 / 不分面。
     */
    public static final BlockLookup<FluidStorage, Direction> BLOCK =
            BlockLookup.get(Key.of("nyana-fluid", "fluid_storage"), FluidStorage.class, Direction.class);
    /**
     * 物品；上下文是物品所在的位置。
     */
    public static final ItemLookup<FluidStorage, ItemAccess> ITEM =
            ItemLookup.get(Key.of("nyana-fluid", "fluid_storage"), FluidStorage.class, ItemAccess.class);
    /**
     * 实体；上下文是面，{@code null} 表示不分面。
     */
    public static final EntityLookup<FluidStorage, Direction> ENTITY =
            EntityLookup.get(Key.of("nyana-fluid", "fluid_storage"), FluidStorage.class, Direction.class);
    /**
     * CE 家具；上下文是面，{@code null} 表示不分面。
     */
    public static final FurnitureLookup<FluidStorage, Direction> FURNITURE =
            FurnitureLookup.get(Key.of("nyana-fluid", "fluid_storage"), FluidStorage.class, Direction.class);

    private FluidLookups() {
    }
}
