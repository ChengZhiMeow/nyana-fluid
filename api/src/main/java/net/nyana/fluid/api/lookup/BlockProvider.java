package net.nyana.fluid.api.lookup;

import net.momirealms.craftengine.core.block.entity.BlockEntity;
import net.momirealms.craftengine.core.world.ExistingBlock;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface BlockProvider<A, C> {
    @Nullable A find(ExistingBlock block, @Nullable BlockEntity blockEntity, @Nullable C context);
}
