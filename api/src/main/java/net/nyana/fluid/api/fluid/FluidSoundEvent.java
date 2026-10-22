package net.nyana.fluid.api.fluid;

import net.momirealms.craftengine.core.util.Key;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 流体相关的音效时机，例如装进容器、从容器倒出。
 *
 * <p>没有用枚举，是为了以后能加新的时机而不破坏调用方。同一个 key 总是返回同一个实例。
 */
public final class FluidSoundEvent {
    /**
     * 流体被装进容器（例如空桶装水）。
     */
    public static final FluidSoundEvent FILL = FluidSoundEvent.of(Key.of("nyana-fluid", "fill"));
    /**
     * 流体从容器里倒出（例如水桶倒进储罐）。
     */
    public static final FluidSoundEvent EMPTY = FluidSoundEvent.of(Key.of("nyana-fluid", "empty"));

    /**
     * 按 key 取音效时机，不存在就创建。
     */
    public static FluidSoundEvent of(Key key) {
        Objects.requireNonNull(key, "key");
        return Values.MAP.computeIfAbsent(key, FluidSoundEvent::new);
    }

    private final Key key;

    private FluidSoundEvent(Key key) {
        this.key = key;
    }

    public Key key() {
        return this.key;
    }

    @Override
    public String toString() {
        return this.key.toString();
    }

    /**
     * 放在单独的类里，第一次用到时才初始化，和上面常量的声明顺序无关。
     */
    private static final class Values {
        private static final Map<Key, FluidSoundEvent> MAP = new ConcurrentHashMap<>();
    }
}
