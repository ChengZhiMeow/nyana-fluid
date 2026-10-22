package net.nyana.fluid.api.internal;

import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * Picks the item model of a gauge level; installed by the plugin once the gauge models are generated.
 */
@ApiStatus.Internal
@FunctionalInterface
public interface GaugeResolver {
    GaugeResolver NONE = (gauge, fluid, fraction) -> null;

    /**
     * {@code fraction} is 0 for an empty gauge; {@code null} means show nothing.
     */
    @Nullable Key model(Key gauge, @Nullable Key fluid, float fraction);
}
