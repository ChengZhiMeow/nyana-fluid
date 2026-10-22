package net.nyana.fluid.api.block;

import net.momirealms.craftengine.core.util.Direction;
import net.nyana.fluid.api.storage.FluidStorage;
import org.jetbrains.annotations.Nullable;

/**
 * 任何 CE {@code BlockEntityController} 实现这个接口，就能被 {@link net.nyana.fluid.api.lookup.FluidLookups#BLOCK}
 * 自动查到，不需要额外注册。同一方块上有多个实现时，按面合并成一个存储。
 */
public interface FluidBlockEntity {
    /**
     * 某个面上暴露的存储。
     */
    @Nullable FluidStorage fluidStorage(@Nullable Direction side);
}
