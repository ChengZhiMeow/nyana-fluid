package net.nyana.fluid.plugin.platform;

import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.world.BlockPos;
import net.nyana.fluid.api.platform.BlockStateBridge;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ReflectiveBlockStateBridge implements BlockStateBridge {
    private final Map<Class<?>, Method> getWorld = new ConcurrentHashMap<>();

    @Override
    public @Nullable BlockLocation locate(Object level, Object position) {
        try {
            org.bukkit.World bukkit = level instanceof org.bukkit.World world ? world : this.bukkitWorld(level);
            if (bukkit == null) return null;
            BlockPos pos = position instanceof BlockPos ce ? ce : LocationUtils.fromBlockPos(position);
            return new BlockLocation(BukkitAdaptor.adapt(bukkit), pos);
        } catch (ReflectiveOperationException | RuntimeException failure) {
            return null;
        }
    }

    private org.bukkit.@Nullable World bukkitWorld(Object level) throws ReflectiveOperationException {
        Method method = this.getWorld.get(level.getClass());
        if (method == null) {
            method = level.getClass().getMethod("getWorld");
            this.getWorld.put(level.getClass(), method);
        }
        return method.invoke(level) instanceof org.bukkit.World world ? world : null;
    }
}
