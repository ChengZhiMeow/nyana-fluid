package net.nyana.fluid.api.lookup;

import net.momirealms.craftengine.core.entity.Entity;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface EntityProvider<A, C> {
    @Nullable A find(Entity entity, @Nullable C context);
}
