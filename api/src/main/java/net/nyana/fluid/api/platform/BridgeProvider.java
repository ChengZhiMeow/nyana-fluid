package net.nyana.fluid.api.platform;

import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 某个 bridge 的一个实现。插件启动时为每个 bridge 挑选「支持当前环境、优先级最高」的实现，并把结果打印到日志。
 */
@ApiStatus.Internal
public interface BridgeProvider<B> {
    /**
     * 用几个函数拼一个实现。
     */
    static <B> BridgeProvider<B> of(String name, int priority, Predicate<PlatformInfo> supports, Function<PlatformInfo, ? extends B> factory) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(supports, "supports");
        Objects.requireNonNull(factory, "factory");
        return new BridgeProvider<>() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public boolean supports(PlatformInfo info) {
                return supports.test(info);
            }

            @Override
            public int priority() {
                return priority;
            }

            @Override
            public B create(PlatformInfo info) {
                return factory.apply(info);
            }

            @Override
            public String toString() {
                return name;
            }
        };
    }

    /**
     * 实现的名字，用于日志和 {@code /nyanafluid platform}。
     */
    String name();

    /**
     * 是否支持当前环境。
     */
    boolean supports(PlatformInfo info);

    /**
     * 优先级，越大越优先。
     */
    int priority();

    /**
     * 创建实现。只在 {@link #supports(PlatformInfo)} 返回 {@code true} 后调用。
     */
    B create(PlatformInfo info);
}
