package net.nyana.fluid.api.item;

import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.world.context.UseOnContext;
import net.nyana.fluid.api.NyanaFluid;
import net.nyana.fluid.api.Registration;
import net.nyana.fluid.api.event.FluidItemInteractEvent;
import net.nyana.fluid.api.fluid.FluidStack;
import net.nyana.fluid.api.fluid.FluidVariant;
import net.nyana.fluid.api.internal.InteractionGuard;
import net.nyana.fluid.api.storage.*;
import net.nyana.fluid.api.transaction.Transaction;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 玩家拿物品右键流体存储：能倒就倒, 能装就装, 交换手上的容器, 不给任何提示,.
 * 成功 SUCCESS_AND_CANCEL, 手上是容器但没转移 FAIL, 不是容器 PASS；接手的点击由插件拦下客户端的后续包,.
 */
public final class FluidInteractions {
    private static final List<HandlerEntry> HANDLERS = new CopyOnWriteArrayList<>();

    /**
     * 处理一次方块交互,.
     */
    public static InteractionResult interact(UseOnContext context, @Nullable FluidStorage target) {
        Player player = context.getPlayer();
        if (player == null || target == null) return InteractionResult.PASS;
        return FluidInteractions.interact(InteractionContext.of(player, context.getHand(), context.getWorld(), context.getClickedPos(), context.getClickedFace()), target);
    }

    /**
     * 处理一次没有方块位置的交互（例如 GUI 里的储罐）,.
     */
    public static InteractionResult interact(Player player, InteractionHand hand, @Nullable FluidStorage target) {
        if (target == null) return InteractionResult.PASS;
        return FluidInteractions.interact(InteractionContext.of(player, hand), target);
    }

    public static InteractionResult interact(InteractionContext context, FluidStorage target) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(target, "target");
        ItemAccess access = ItemAccess.ofInteraction(context.player(), context.hand()).oneByOne();
        for (HandlerEntry entry : FluidInteractions.HANDLERS) {
            InteractionResult result = entry.handler.handle(access, target, context);
            if (result != InteractionResult.PASS) {
                FluidInteractions.guardFollowUp(context);
                return result;
            }
        }
        FluidStorage item = access.fluidStorage();
        if (item == null) return InteractionResult.PASS;
        // The click is ours now: stop the client's follow-up item use (see InteractionGuard).
        FluidInteractions.guardFollowUp(context);
        TransferResult result = FluidInteractions.transfer(context, item, target, target, FluidItemInteractEvent.Mode.EMPTY_ITEM);
        if (!result.success() && result.status() != TransferResult.Status.CANCELLED) {
            result = FluidInteractions.transfer(context, target, item, target, FluidItemInteractEvent.Mode.FILL_ITEM);
        }
        return result.success() ? InteractionResult.SUCCESS_AND_CANCEL : InteractionResult.FAIL;
    }

    /**
     * 把物品里的流体倒进存储,.
     */
    public static TransferResult emptyItemInto(ItemAccess item, FluidStorage target, long maxAmount, FluidAction action) {
        FluidStorage source = item.fluidStorage();
        if (source == null) return TransferResult.failure(TransferResult.Status.UNSUPPORTED);
        return FluidTransfer.tryMove(source, target, variant -> true, maxAmount, action);
    }

    public static TransferResult fillItemFrom(ItemAccess item, FluidStorage source, long maxAmount, FluidAction action) {
        FluidStorage target = item.fluidStorage();
        if (target == null) return TransferResult.failure(TransferResult.Status.UNSUPPORTED);
        return FluidTransfer.tryMove(source, target, variant -> true, maxAmount, action);
    }

    /**
     * 注册自定义交互规则,.
     */
    public static Registration registerHandler(Plugin owner, int priority, InteractionHandler handler) {
        HandlerEntry entry = new HandlerEntry(priority, Objects.requireNonNull(handler, "handler"));
        synchronized (FluidInteractions.HANDLERS) {
            List<HandlerEntry> sorted = new ArrayList<>(FluidInteractions.HANDLERS);
            sorted.add(entry);
            sorted.sort(Comparator.comparingInt((HandlerEntry e) -> e.priority).reversed());
            FluidInteractions.HANDLERS.clear();
            FluidInteractions.HANDLERS.addAll(sorted);
        }
        return NyanaFluid.service().registrations().register(owner, () -> FluidInteractions.HANDLERS.remove(entry));
    }

    private static TransferResult transfer(InteractionContext context, FluidStorage from, FluidStorage to, FluidStorage target,
                                           FluidItemInteractEvent.Mode mode) {
        if (from.isLocked() || to.isLocked()) return TransferResult.failure(TransferResult.Status.LOCKED_DATA);
        boolean found = false;
        try (Transaction transaction = Transaction.open(Transaction.current())) {
            for (FluidSlotView view : from.nonEmptyViews()) {
                FluidVariant variant = view.variant();
                if (variant.isBlank()) continue;
                found = true;
                long moved = FluidTransfer.move(from, to, variant::equals, Long.MAX_VALUE, transaction);
                if (moved <= 0) continue;
                FluidItemInteractEvent event = new FluidItemInteractEvent(context, target, mode, FluidStack.of(variant, moved));
                NyanaFluid.service().callEvent(event);
                if (event.isCancelled()) return TransferResult.failure(TransferResult.Status.CANCELLED);
                transaction.commit();
                return TransferResult.moved(variant, moved);
            }
        }
        return TransferResult.failure(found ? TransferResult.Status.REJECTED : TransferResult.Status.SOURCE_EMPTY);
    }

    private static void guardFollowUp(InteractionContext context) {
        if (context.player().platformPlayer() instanceof org.bukkit.entity.Player bukkit) {
            InteractionGuard.mark(bukkit.getUniqueId(), context.hand(), context.player().gameTicks());
        }
    }

    private FluidInteractions() {
    }

    private record HandlerEntry(int priority, InteractionHandler handler) {
    }
}
