package net.nyana.fluid.api.platform;

import net.nyana.fluid.api.ApiVersion;

/**
 * 当前运行环境的信息。addon 可以用它做版本判断；需要按版本切换实现的地方请走 bridge。
 */
public interface PlatformInfo {
    /**
     * Minecraft 版本，例如 {@code 1.21.11}。
     */
    String minecraftVersion();

    /**
     * CraftEngine 版本。
     */
    ApiVersion craftEngineVersion();

    /**
     * 是否运行在 Folia 上。
     */
    boolean folia();

    /**
     * 运行时的 Java 主版本号。
     */
    int javaVersion();

    /**
     * CE 是否提供原生 sleeping ticker（CE 26.10+）。
     */
    boolean nativeSleepingTicker();
}
