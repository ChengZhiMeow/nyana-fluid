package net.nyana.fluid.plugin;

import net.nyana.fluid.api.block.FluidTankController;
import net.nyana.fluid.api.event.FluidRegistryChangeEvent;
import net.nyana.fluid.api.fluid.FluidRegistry;
import net.nyana.fluid.api.fluid.FluidRegistryChange;
import net.nyana.fluid.api.fluid.component.FluidComponentRegistry;
import net.nyana.fluid.api.internal.Core;
import net.nyana.fluid.api.internal.NyanaFluidService;
import net.nyana.fluid.api.internal.Registrations;
import net.nyana.fluid.api.item.FluidContainers;
import net.nyana.fluid.api.persist.FluidCodec;
import net.nyana.fluid.api.platform.PlatformInfo;
import net.nyana.fluid.api.recipe.FluidIngredientRegistry;
import net.nyana.fluid.plugin.flush.FluidDirtyQueue;
import net.nyana.fluid.plugin.platform.PluginBridge;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;

final class PluginService implements NyanaFluidService {
    private final Main plugin;
    private final PlatformInfo platform;
    private final PluginBridge bridge;
    private final FluidDirtyQueue dirtyQueue;
    private volatile boolean ready;

    PluginService(Main plugin, PlatformInfo platform, PluginBridge bridge) {
        this.plugin = plugin;
        this.platform = platform;
        this.bridge = bridge;
        this.dirtyQueue = new FluidDirtyQueue(plugin, bridge.scheduler(), this);
    }

    @Override
    public FluidRegistry fluids() {
        return Core.get().fluids;
    }

    @Override
    public FluidComponentRegistry components() {
        return Core.get().components;
    }

    @Override
    public FluidIngredientRegistry ingredients() {
        return Core.get().ingredients;
    }

    @Override
    public FluidContainers containers() {
        return Core.get().containers;
    }

    @Override
    public FluidCodec codec() {
        return Core.get().codec;
    }

    @Override
    public PlatformInfo platform() {
        return this.platform;
    }

    @Override
    public PluginBridge bridge() {
        return this.bridge;
    }

    @Override
    public Registrations registrations() {
        return Core.get().registrations;
    }

    @Override
    public void markChanged(FluidTankController controller) {
        this.dirtyQueue.mark(controller);
    }

    @Override
    public void callEvent(Event event) {
        if (!this.plugin.isEnabled()) return;
        if (this.platform.folia() || Bukkit.isPrimaryThread()) Bukkit.getPluginManager().callEvent(event);
        else this.bridge.scheduler().runGlobal(() -> Bukkit.getPluginManager().callEvent(event));
    }

    void onRegistryChange(FluidRegistryChange change) {
        if (!this.plugin.isEnabled()) return;
        this.bridge.scheduler().runGlobal(() -> {
            if (this.plugin.isEnabled()) Bukkit.getPluginManager().callEvent(new FluidRegistryChangeEvent(change));
        });
    }

    @Override
    public boolean ready() {
        return this.ready;
    }

    void setReady(boolean ready) {
        this.ready = ready;
    }
}
