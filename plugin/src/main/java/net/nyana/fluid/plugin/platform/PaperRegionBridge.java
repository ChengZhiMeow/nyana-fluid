package net.nyana.fluid.plugin.platform;

import net.nyana.fluid.api.platform.RegionBridge;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;

public final class PaperRegionBridge implements RegionBridge {
    @Override
    public boolean ownsChunk(World world, int chunkX, int chunkZ) {
        return Bukkit.isOwnedByCurrentRegion(world, chunkX, chunkZ);
    }

    @Override
    public boolean ownsEntity(Entity entity) {
        return Bukkit.isOwnedByCurrentRegion(entity);
    }
}
