package net.nyana.fluid.platform.ce2610;

import net.momirealms.craftengine.core.block.entity.BlockEntityController;
import net.momirealms.craftengine.core.block.entity.tick.BlockEntityTicker;
import net.momirealms.craftengine.core.block.entity.tick.SleepingBlockEntityTicker;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.chunk.ChunkSubscription;
import net.nyana.fluid.api.Registration;
import net.nyana.fluid.api.internal.Core;
import net.nyana.fluid.api.platform.TickHandle;
import net.nyana.fluid.api.platform.TickerBridge;
import org.bukkit.plugin.Plugin;

import java.util.Objects;

public final class NativeTickerBridge implements TickerBridge {
    private final Plugin plugin;

    public NativeTickerBridge(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    public <C extends BlockEntityController> TickHandle<C> create(BlockEntityTicker<C> delegate) {
        SleepingBlockEntityTicker<C> ticker = new SleepingBlockEntityTicker<>(delegate);
        return new TickHandle<>() {
            @Override
            public BlockEntityTicker<C> ticker() {
                return ticker;
            }

            @Override
            public boolean awake() {
                return !ticker.isSleeping();
            }

            @Override
            public void wake() {
                ticker.wakeUp();
            }

            @Override
            public void sleep() {
                ticker.sleep();
            }
        };
    }

    @Override
    public Registration subscribeChunkLoad(BlockEntityController controller, BlockPos target, Runnable callback) {
        ChunkSubscription subscription = controller.subscribeChunkLoad(target, callback);
        return Core.get().registrations.register(this.plugin, subscription::cancel);
    }

    @Override
    public boolean nativeSleep() {
        return true;
    }
}
