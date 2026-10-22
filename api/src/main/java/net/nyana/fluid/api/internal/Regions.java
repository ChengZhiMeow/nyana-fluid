package net.nyana.fluid.api.internal;

import net.nyana.fluid.api.NyanaFluid;
import net.nyana.fluid.api.platform.PlatformBridge;
import net.nyana.fluid.api.transaction.FluidAccessException;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.ApiStatus;

/**
 * Region ownership checks through the RegionBridge, or Paper directly when no plugin is installed.
 */
@ApiStatus.Internal
public final class Regions {
    public static boolean ownsChunk(World world, int chunkX, int chunkZ) {
        PlatformBridge bridge = NyanaFluid.service().bridge();
        if (bridge != null) return bridge.region().ownsChunk(world, chunkX, chunkZ);
        return Bukkit.getServer() == null || Bukkit.isOwnedByCurrentRegion(world, chunkX, chunkZ);
    }

    public static boolean ownsEntity(Entity entity) {
        PlatformBridge bridge = NyanaFluid.service().bridge();
        if (bridge != null) return bridge.region().ownsEntity(entity);
        return Bukkit.getServer() == null || Bukkit.isOwnedByCurrentRegion(entity);
    }

    public static void checkChunk(World world, int chunkX, int chunkZ) {
        if (!Regions.ownsChunk(world, chunkX, chunkZ)) {
            throw new FluidAccessException("Chunk " + chunkX + "," + chunkZ + " in " + world.getName() + " is not owned by the current thread");
        }
    }

    public static void checkEntity(Entity entity) {
        if (!Regions.ownsEntity(entity))
            throw new FluidAccessException(entity.getName() + " is not owned by the current thread");
    }

    private Regions() {
    }
}
