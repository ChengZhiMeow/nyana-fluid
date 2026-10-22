package net.nyana.fluid.plugin.flush;

import net.momirealms.craftengine.core.block.entity.BlockEntity;
import net.nyana.fluid.api.block.FluidTankController;
import net.nyana.fluid.api.event.FluidStorageChangedEvent;
import net.nyana.fluid.api.internal.NyanaFluidService;
import net.nyana.fluid.api.platform.SchedulerBridge;
import org.bukkit.plugin.Plugin;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class FluidDirtyQueue {
    private final Plugin plugin;
    private final SchedulerBridge scheduler;
    private final NyanaFluidService service;
    private final Set<FluidTankController> pending = ConcurrentHashMap.newKeySet();

    public FluidDirtyQueue(Plugin plugin, SchedulerBridge scheduler, NyanaFluidService service) {
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.service = service;
    }

    public void mark(FluidTankController controller) {
        BlockEntity entity = controller.blockEntity();
        if (entity == null || entity.world == null || !this.plugin.isEnabled()) return;
        if (!this.pending.add(controller)) return;
        this.scheduler.runAt(entity.world.world(), entity.pos.x() >> 4, entity.pos.z() >> 4, () -> this.flush(controller));
    }

    private void flush(FluidTankController controller) {
        this.pending.remove(controller);
        try {
            if (!controller.flush()) return;
            BlockEntity entity = controller.blockEntity();
            this.service.callEvent(new FluidStorageChangedEvent(entity.world.world(), entity.pos, controller.tanks(), controller.tanks().version()));
        } catch (RuntimeException failure) {
            this.plugin.getLogger().log(Level.WARNING, "Failed to flush fluid tank changes", failure);
        }
    }
}
