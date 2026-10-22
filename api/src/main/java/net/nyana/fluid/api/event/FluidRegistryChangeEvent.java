package net.nyana.fluid.api.event;

import net.nyana.fluid.api.fluid.FluidRegistryChange;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * 流体注册表变化：注册、更新、注销，或者 CE reload。在主线程（Paper）或全局区域线程（Folia）上派发。不可取消。
 */
public class FluidRegistryChangeEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * Bukkit 要求的静态 handler 列表。
     */
    public static HandlerList getHandlerList() {
        return FluidRegistryChangeEvent.HANDLERS;
    }

    private final FluidRegistryChange change;

    /**
     * 只由 NyanaFluid 调用。
     */
    @ApiStatus.Internal
    public FluidRegistryChangeEvent(FluidRegistryChange change) {
        this.change = change;
    }

    public FluidRegistryChange change() {
        return this.change;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return FluidRegistryChangeEvent.HANDLERS;
    }
}
