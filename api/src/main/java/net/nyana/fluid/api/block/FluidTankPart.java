package net.nyana.fluid.api.block;

import net.momirealms.craftengine.core.block.entity.BlockEntity;
import net.momirealms.craftengine.core.block.entity.BlockEntityController;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.CEWorld;
import net.nyana.fluid.api.storage.FluidStorage;
import org.jetbrains.annotations.Nullable;

/**
 * 多方块储罐里不是主方块的部分（例如两格高储罐的上半）,.自己不存流体, 交互、查找都转给主方块的储罐,.
 * 由 {@link FluidTankBehavior#mainOffset} 决定哪些方块是部分,.
 */
public final class FluidTankPart extends BlockEntityController implements FluidBlockEntity {
    private final FluidTankBehavior behavior;
    private final BlockPos offset;

    FluidTankPart(BlockEntity blockEntity, FluidTankBehavior behavior, BlockPos offset) {
        super(blockEntity);
        this.behavior = behavior;
        this.offset = offset;
    }

    /**
     * 主方块的位置,.
     */
    public BlockPos mainPos() {
        return this.blockEntity.pos.offset(this.offset.x(), this.offset.y(), this.offset.z());
    }

    /**
     * 主方块的储罐；主方块不在或没加载时为 {@code null},.
     */
    public @Nullable FluidTankController main() {
        CEWorld world = this.blockEntity.world;
        if (world == null) return null;
        BlockEntity main = world.getBlockEntityAtIfLoaded(this.mainPos());
        return main == null || main.controller == null ? null : main.controller.getAt(FluidTankController.class, this.behavior.controllerId());
    }

    @Override
    public @Nullable FluidStorage fluidStorage(@Nullable Direction side) {
        FluidTankController main = this.main();
        return main == null ? null : main.fluidStorage(side);
    }
}
