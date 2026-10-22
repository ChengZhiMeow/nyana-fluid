package net.nyana.fluid.plugin.platform;

import net.nyana.fluid.api.ApiVersion;
import net.nyana.fluid.api.platform.PlatformInfo;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public record PaperPlatformInfo(String minecraftVersion, ApiVersion craftEngineVersion, boolean folia, int javaVersion,
                                boolean nativeSleepingTicker) implements PlatformInfo {
    public static final String SLEEPING_TICKER = "net.momirealms.craftengine.core.block.entity.tick.SleepingBlockEntityTicker";

    public static PaperPlatformInfo detect(ClassLoader craftEngineLoader) {
        Plugin craftEngine = Bukkit.getPluginManager().getPlugin("CraftEngine");
        ApiVersion ceVersion = craftEngine == null ? ApiVersion.of(0, 0, 0) : ApiVersion.parse(craftEngine.getPluginMeta().getVersion());
        return new PaperPlatformInfo(Bukkit.getMinecraftVersion(), ceVersion, PaperPlatformInfo.classExists("io.papermc.paper.threadedregions.RegionizedServer", PaperPlatformInfo.class.getClassLoader()),
                Runtime.version().feature(), PaperPlatformInfo.classExists(PaperPlatformInfo.SLEEPING_TICKER, craftEngineLoader));
    }

    public static boolean classExists(String name, ClassLoader loader) {
        try {
            Class.forName(name, false, loader);
            return true;
        } catch (ClassNotFoundException | LinkageError missing) {
            return false;
        }
    }
}
