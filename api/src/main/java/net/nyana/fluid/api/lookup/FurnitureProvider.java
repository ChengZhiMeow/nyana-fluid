package net.nyana.fluid.api.lookup;

import net.momirealms.craftengine.core.entity.furniture.Furniture;
import org.jetbrains.annotations.Nullable;

/**
 * 按 CE 家具提供能力,.
 */
@FunctionalInterface
public interface FurnitureProvider<A, C> {
    @Nullable A find(Furniture furniture, @Nullable C context);
}
