package net.nyana.fluid.api.internal;

import org.jetbrains.annotations.ApiStatus;

/**
 * The registries every service shares. They live here rather than in a service so that registrations an addon makes
 * in {@code onLoad} (before the plugin installs its service) survive the switch from the standalone service.
 */
@ApiStatus.Internal
public final class Core {
    private static final Core INSTANCE = new Core();

    public static Core get() {
        return Core.INSTANCE;
    }

    public final Registrations registrations = new Registrations();
    public final SimpleFluidRegistry fluids = new SimpleFluidRegistry(this.registrations);
    public final SimpleComponentRegistry components = new SimpleComponentRegistry(this.registrations);
    public final NbtFluidCodec codec = new NbtFluidCodec(this.fluids, this.components, this.registrations);
    public final SimpleIngredientRegistry ingredients = new SimpleIngredientRegistry(this.registrations);
    public final SimpleFluidContainers containers = new SimpleFluidContainers(this.registrations);
    public volatile GaugeResolver gauges = GaugeResolver.NONE;

    private Core() {
    }
}
