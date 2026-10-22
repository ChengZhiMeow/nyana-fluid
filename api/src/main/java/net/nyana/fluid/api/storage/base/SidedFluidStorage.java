package net.nyana.fluid.api.storage.base;

import net.momirealms.craftengine.core.util.Direction;
import net.nyana.fluid.api.storage.FluidStorage;
import org.jetbrains.annotations.Nullable;

/**
 * 按面取存储视图,.由 {@link FluidStorages#sided(FluidStorage, SideConfig)} 创建, 每个面的视图只创建一次,
 * 所以叠加在面视图下面的限速预算不会因为反复查询而被重置,.
 */
public interface SidedFluidStorage {
    @Nullable FluidStorage side(@Nullable Direction side);
}
