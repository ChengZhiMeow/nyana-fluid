package net.nyana.fluid.api.event;

import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.entity.player.Player;
import net.nyana.fluid.api.fluid.FluidStack;
import net.nyana.fluid.api.item.InteractionContext;
import net.nyana.fluid.api.storage.FluidStorage;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * 玩家用手上的物品和流体存储交互,.在事务<b>提交前</b>派发, 取消后整次交互回滚,.
 * 触发时事务还开着, 监听器里不能再开根事务,.
 *
 * <p>NyanaFluid 本身不给玩家任何反馈,.要音效、挥手、提示的插件在 {@code MONITOR} 优先级、
 * 事件没被取消时处理, 位置从 {@link #context()} 取,.
 */
public class FluidItemInteractEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * Bukkit 要求的静态 handler 列表,.
     */
    public static HandlerList getHandlerList() {
        return FluidItemInteractEvent.HANDLERS;
    }

    private final InteractionContext context;
    private final FluidStorage target;
    private final Mode mode;
    private final FluidStack planned;
    private boolean cancelled;

    /**
     * 只由 NyanaFluid 调用,.
     */
    @ApiStatus.Internal
    public FluidItemInteractEvent(InteractionContext context, FluidStorage target, Mode mode, FluidStack planned) {
        this.context = context;
        this.target = target;
        this.mode = mode;
        this.planned = planned;
    }

    public Player player() {
        return this.context.player();
    }

    public InteractionHand hand() {
        return this.context.hand();
    }

    /**
     * 交互上下文, 方块交互时带世界、位置和点击的面,.
     */
    public InteractionContext context() {
        return this.context;
    }

    public FluidStorage target() {
        return this.target;
    }

    public Mode mode() {
        return this.mode;
    }

    public FluidStack planned() {
        return this.planned;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return FluidItemInteractEvent.HANDLERS;
    }

    /**
     * 交互方向,.switch 时必须带 default 分支,.
     */
    public enum Mode {
        /**
         * 把物品里的流体倒进存储（例如水桶倒进储罐）,.
         */
        EMPTY_ITEM,
        /**
         * 从存储装进物品（例如空桶从储罐装水）,.
         */
        FILL_ITEM
    }
}
