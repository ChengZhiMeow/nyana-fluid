package net.nyana.fluid.api.internal;

import net.nyana.fluid.api.ApiVersion;
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
 * Service used when the NyanaFluid plugin is not installed (before the plugin enables, e.g. while CE parses config packs, and outside a server): the shared {@link Core}, no bridges, no events.
 */
@ApiStatus.Internal
public final class StandaloneService implements NyanaFluidService {
    private final Core core = Core.get();
    private final PlatformInfo platform = new StandalonePlatform();

    @Override
    public FluidRegistry fluids() {
        return this.core.fluids;
    }

    @Override
    public FluidComponentRegistry components() {
        return this.core.components;
    }

    @Override
    public FluidIngredientRegistry ingredients() {
        return this.core.ingredients;
    }

    @Override
    public FluidContainers containers() {
        return this.core.containers;
    }

    @Override
    public FluidCodec codec() {
        return this.core.codec;
    }

    @Override
    public PlatformInfo platform() {
        return this.platform;
    }

    @Override
    public @Nullable PlatformBridge bridge() {
        return null;
    }

    @Override
    public Registrations registrations() {
        return this.core.registrations;
    }

    @Override
    public void markChanged(FluidTankController controller) {
    }

    @Override
    public void callEvent(Event event) {
    }

    @Override
    public boolean ready() {
        return false;
    }

    private static final class StandalonePlatform implements PlatformInfo {
        @Override
        public String minecraftVersion() {
            return "unknown";
        }

        @Override
        public ApiVersion craftEngineVersion() {
            return ApiVersion.of(0, 0, 0);
        }

        @Override
        public boolean folia() {
            return false;
        }

        @Override
        public int javaVersion() {
            return Runtime.version().feature();
        }

        @Override
        public boolean nativeSleepingTicker() {
            return false;
        }
    }
}
