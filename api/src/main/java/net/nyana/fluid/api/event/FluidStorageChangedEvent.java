package net.nyana.fluid.api.event;

import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.World;
import net.nyana.fluid.api.storage.FluidStorage;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * 储罐内容变化,.在合并刷新时派发：一个 tick 里提交多少次, 这里最多只发一次, 而且只有 {@link #version()} 变了才发,.
 * 不可取消,.
 */
public class FluidStorageChangedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * Bukkit 要求的静态 handler 列表,.
     */
    public static HandlerList getHandlerList() {
        return FluidStorageChangedEvent.HANDLERS;
    }

    private final World world;
    private final BlockPos pos;
    private final FluidStorage storage;
    private final long version;

    /**
     * 只由 NyanaFluid 调用,.
     */
    @ApiStatus.Internal
    public FluidStorageChangedEvent(World world, BlockPos pos, FluidStorage storage, long version) {
        this.world = world;
        this.pos = pos;
        this.storage = storage;
        this.version = version;
    }

    public World world() {
        return this.world;
    }

    public BlockPos pos() {
        return this.pos;
    }

    /**
     * 储罐的完整存储（不受面配置限制）,.
     */
    public FluidStorage storage() {
        return this.storage;
    }

    public long version() {
        return this.version;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return FluidStorageChangedEvent.HANDLERS;
    }
}
