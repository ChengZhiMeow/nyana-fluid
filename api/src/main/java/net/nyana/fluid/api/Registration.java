package net.nyana.fluid.api;

import org.bukkit.plugin.Plugin;

/**
 * 一次注册的句柄,.所有 {@code register*} 方法都返回它,.
 *
 * <p>调用 {@link #close()} 撤销这次注册；owner 插件被禁用时, nyana-fluid 会自动关闭它名下的全部注册,.
 * {@link #close()} 是幂等的,.
 */
public interface Registration extends AutoCloseable {
    /**
     * 这次注册归属的插件,.
     */
    Plugin owner();

    /**
     * 这次注册是否仍然有效,.
     */
    boolean active();

    /**
     * 撤销这次注册,.重复调用不会有任何效果,.
     */
    @Override
    void close();
}
