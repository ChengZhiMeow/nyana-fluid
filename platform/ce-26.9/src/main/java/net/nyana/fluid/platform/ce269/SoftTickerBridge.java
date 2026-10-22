package net.nyana.fluid.platform.ce269;

import net.momirealms.craftengine.core.block.entity.BlockEntity;
import net.momirealms.craftengine.core.block.entity.BlockEntityController;
import net.momirealms.craftengine.core.block.entity.tick.BlockEntityTicker;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.CEWorld;
import net.momirealms.craftengine.core.world.World;
import net.nyana.fluid.api.Registration;
import net.nyana.fluid.api.internal.Core;
import net.nyana.fluid.api.internal.SoftTickHandle;
import net.nyana.fluid.api.platform.SchedulerBridge;
import net.nyana.fluid.api.platform.TickHandle;
import net.nyana.fluid.api.platform.TickerBridge;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class SoftTickerBridge implements TickerBridge, Listener {
    private final Plugin plugin;
    private final SchedulerBridge scheduler;
    private final Map<ChunkKey, List<Subscription>> subscriptions = new ConcurrentHashMap<>();

    public SoftTickerBridge(Plugin plugin, SchedulerBridge scheduler) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public <C extends BlockEntityController> TickHandle<C> create(BlockEntityTicker<C> delegate) {
        return SoftTickHandle.create(delegate);
    }

    @Override
    public Registration subscribeChunkLoad(BlockEntityController controller, BlockPos target, Runnable callback) {
        BlockEntity owner = Objects.requireNonNull(controller.blockEntity(), "controller has no block entity");
        CEWorld world = Objects.requireNonNull(owner.world, "block entity has no world");
        ChunkKey key = new ChunkKey(world.uuid(), target.x() >> 4, target.z() >> 4);
        Subscription subscription = new Subscription(world.world(), owner.pos.x() >> 4, owner.pos.z() >> 4, callback);
        this.subscriptions.computeIfAbsent(key, ignored -> new CopyOnWriteArrayList<>()).add(subscription);
        if (world.world().getChunkIfLoaded(key.x, key.z) != null) this.dispatch(subscription);
        return Core.get().registrations.register(this.plugin, () -> {
            subscription.active = false;
            List<Subscription> list = this.subscriptions.get(key);
            if (list != null) {
                list.remove(subscription);
                if (list.isEmpty()) this.subscriptions.remove(key, list);
            }
        });
    }

    @Override
    public boolean nativeSleep() {
        return false;
    }

    /**
     * Stops listening; called when the plugin disables.
     */
    public void shutdown() {
        HandlerList.unregisterAll(this);
        this.subscriptions.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        List<Subscription> list = this.subscriptions.get(new ChunkKey(event.getWorld().getUID(), event.getChunk().getX(), event.getChunk().getZ()));
        if (list == null) return;
        for (Subscription subscription : list) this.dispatch(subscription);
    }

    private void dispatch(Subscription subscription) {
        this.scheduler.runAt(subscription.ownerWorld, subscription.ownerChunkX, subscription.ownerChunkZ, () -> {
            if (subscription.active) subscription.callback.run();
        });
    }

    private record ChunkKey(UUID world, int x, int z) {
    }

    private static final class Subscription {
        private final World ownerWorld;
        private final int ownerChunkX;
        private final int ownerChunkZ;
        private final Runnable callback;
        private volatile boolean active = true;

        private Subscription(World ownerWorld, int ownerChunkX, int ownerChunkZ, Runnable callback) {
            this.ownerWorld = ownerWorld;
            this.ownerChunkX = ownerChunkX;
            this.ownerChunkZ = ownerChunkZ;
            this.callback = callback;
        }
    }
}
