package net.nyana.fluid.api.lookup;

import net.momirealms.craftengine.core.item.Item;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface ItemProvider<A, C> {
    @Nullable A find(Item item, @Nullable C context);
}
