package net.nyana.fluid.api.internal;

import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.libraries.nbt.Tag;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Tank data captured when a tank block is removed, so the loot function can still find it after CE has dropped the
 * block entity. Entries are taken once and expire after a few seconds.
 */
@ApiStatus.Internal
public final class DropCache {
    private static final long TTL_NANOS = TimeUnit.SECONDS.toNanos(5);
    private static final Map<Location, Entry> ENTRIES = new ConcurrentHashMap<>();

    public static void put(UUID world, BlockPos pos, Tag data) {
        DropCache.purge();
        DropCache.ENTRIES.put(new Location(world, pos.x(), pos.y(), pos.z()), new Entry(data.deepClone(), System.nanoTime() + DropCache.TTL_NANOS));
    }

    public static @Nullable Tag take(UUID world, int x, int y, int z) {
        Entry entry = DropCache.ENTRIES.remove(new Location(world, x, y, z));
        return entry == null || entry.expires < System.nanoTime() ? null : entry.data;
    }

    private static void purge() {
        long now = System.nanoTime();
        DropCache.ENTRIES.values().removeIf(entry -> entry.expires < now);
    }

    private DropCache() {
    }

    private record Location(UUID world, int x, int y, int z) {
    }

    private record Entry(Tag data, long expires) {
    }
}
