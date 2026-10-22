package net.nyana.fluid.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * NyanaFluid 插件启用完成，{@link net.nyana.fluid.api.NyanaFluid#isReady()} 变为 {@code true}。在主线程 / 全局区域线程上派发。
 */
public class NyanaFluidReadyEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * Bukkit 要求的静态 handler 列表。
     */
    public static HandlerList getHandlerList() {
        return NyanaFluidReadyEvent.HANDLERS;
    }

    /**
     * 只由 NyanaFluid 调用。
     */
    @ApiStatus.Internal
    public NyanaFluidReadyEvent() {
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return NyanaFluidReadyEvent.HANDLERS;
    }
}
