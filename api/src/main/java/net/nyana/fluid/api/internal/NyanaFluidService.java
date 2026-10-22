package net.nyana.fluid.api.internal;

import net.nyana.fluid.api.block.FluidTankController;
import net.nyana.fluid.api.fluid.FluidRegistry;
import net.nyana.fluid.api.fluid.component.FluidComponentRegistry;
import net.nyana.fluid.api.item.FluidContainers;
import net.nyana.fluid.api.persist.FluidCodec;
import net.nyana.fluid.api.platform.PlatformBridge;
import net.nyana.fluid.api.platform.PlatformInfo;
import net.nyana.fluid.api.recipe.FluidIngredientRegistry;
import org.bukkit.event.Event;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * The runtime behind {@link net.nyana.fluid.api.NyanaFluid}: installed by the plugin, or {@link StandaloneService} otherwise.
 */
@ApiStatus.Internal
public interface NyanaFluidService {
    FluidRegistry fluids();

    FluidComponentRegistry components();

    FluidIngredientRegistry ingredients();

    FluidContainers containers();

    FluidCodec codec();

    PlatformInfo platform();

    /**
     * Version bridges; {@code null} when no plugin is installed.
     */
    @Nullable PlatformBridge bridge();

    Registrations registrations();

    /**
     * Queues a coalesced flush (save, visual, event) for a tank whose contents changed in a committed root transaction.
     */
    void markChanged(FluidTankController controller);

    /**
     * Fires a Bukkit event on a thread that may fire it.
     */
    void callEvent(Event event);

    boolean ready();
}
