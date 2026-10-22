package net.nyana.fluid.api.render;

import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.*;

/**
 * 储罐里流体展示实体的设置，方块行为配置里的 {@code renderer:}：
 *
 * <pre>
 * renderer:
 *   bounds: [1, 1, 1, 15, 15, 15]   # 液体占的盒子，像素
 *   levels: 16                       # 液面分几档，最多 16
 *   transition_ticks: 5              # 液面变化的动画时长，0 不做动画
 *   transition_delay_ticks: 2
 *   view_range: 1.0
 *   brightness: -1                   # -1 按流体发光等级
 *   translation: [0, 0, 0]           # 额外平移，方块
 *   scale: [1, 1, 1]                 # 额外缩放
 *   rotation: [0, 0, 0]              # 额外旋转，角度
 *   fluids:
 *     minecraft:lava:
 *       model: mypack:tank/lava_{height}   # 自定义物品模型，{height} 是 1..16 像素高的那一档；不写 {height} 就整体缩放
 *       brightness: 15
 * </pre>
 */
public final class TankRenderSpec {
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 解析 {@code renderer:} 配置节，键见类说明。
     */
    public static TankRenderSpec parse(Map<?, ?> section) {
        Builder builder = TankRenderSpec.builder();
        if (section.get("bounds") instanceof List<?> list) builder.bounds(TankRenderSpec.floats(list, 6, "bounds"));
        if (section.get("levels") instanceof Number number) builder.levels(number.intValue());
        if (section.get("transition_ticks") instanceof Number number) builder.transitionTicks(number.intValue());
        if (section.get("transition_delay_ticks") instanceof Number number)
            builder.transitionDelayTicks(number.intValue());
        if (section.get("view_range") instanceof Number number) builder.viewRange(number.floatValue());
        if (section.get("brightness") instanceof Number number) builder.brightness(number.intValue());
        if (section.get("translation") instanceof List<?> list)
            builder.translation(TankRenderSpec.vector(list, "translation"));
        if (section.get("scale") instanceof List<?> list) builder.scale(TankRenderSpec.vector(list, "scale"));
        if (section.get("rotation") instanceof List<?> list) builder.rotation(TankRenderSpec.vector(list, "rotation"));
        if (section.get("fluids") instanceof Map<?, ?> fluids) {
            fluids.forEach((id, value) -> {
                if (!(value instanceof Map<?, ?> fluid)) return;
                String model = fluid.get("model") == null ? null : String.valueOf(fluid.get("model"));
                int light = fluid.get("brightness") instanceof Number number ? number.intValue() : -1;
                builder.fluid(Key.of(String.valueOf(id)), new FluidRender(model, light));
            });
        }
        return builder.build();
    }

    private static float[] floats(List<?> list, int size, String name) {
        if (list.size() != size) throw new IllegalArgumentException(name + " needs " + size + " numbers");
        float[] values = new float[size];
        for (int i = 0; i < size; i++) values[i] = ((Number) list.get(i)).floatValue();
        return values;
    }

    private static Vector3f vector(List<?> list, String name) {
        float[] values = TankRenderSpec.floats(list, 3, name);
        return new Vector3f(values[0], values[1], values[2]);
    }

    private final float[] bounds;
    private final int levels;
    private final int transitionTicks;
    private final int transitionDelayTicks;
    private final float viewRange;
    private final int brightness;
    private final Vector3f translation;
    private final Vector3f scale;
    private final Vector3f rotation;
    private final Map<Key, FluidRender> fluids;

    private TankRenderSpec(Builder builder) {
        float[] b = builder.bounds;
        if (b[0] >= b[3] || b[1] >= b[4] || b[2] >= b[5])
            throw new IllegalArgumentException("Renderer bounds must have a positive size");
        if (builder.levels < 1 || builder.levels > 16)
            throw new IllegalArgumentException("Renderer levels must be within [1, 16]");
        this.bounds = b.clone();
        this.levels = builder.levels;
        this.transitionTicks = Math.max(0, builder.transitionTicks);
        this.transitionDelayTicks = Math.max(1, builder.transitionDelayTicks);
        this.viewRange = builder.viewRange;
        this.brightness = builder.brightness;
        this.translation = new Vector3f(builder.translation);
        this.scale = new Vector3f(builder.scale);
        this.rotation = new Vector3f(builder.rotation);
        this.fluids = Map.copyOf(builder.fluids);
    }

    /**
     * 液体盒子 {@code [minX, minY, minZ, maxX, maxY, maxZ]}，方块坐标 0..1。
     */
    public float[] bounds() {
        return this.bounds.clone();
    }

    public int levels() {
        return this.levels;
    }

    public int transitionTicks() {
        return this.transitionTicks;
    }

    public int transitionDelayTicks() {
        return this.transitionDelayTicks;
    }

    public float viewRange() {
        return this.viewRange;
    }

    /**
     * 固定亮度 0..15；-1 表示按流体的发光等级。
     */
    public int brightness() {
        return this.brightness;
    }

    public Vector3f translation() {
        return new Vector3f(this.translation);
    }

    public Vector3f scale() {
        return new Vector3f(this.scale);
    }

    /**
     * 欧拉角，角度。
     */
    public Vector3f rotation() {
        return new Vector3f(this.rotation);
    }

    public Optional<FluidRender> fluid(Key fluid) {
        return Optional.ofNullable(this.fluids.get(fluid));
    }

    /**
     * 某种流体的自定义展示。
     *
     * @param model      物品模型模板，{@code {height}} 换成 1..16；{@code null} 用生成的默认模型
     * @param brightness 固定亮度 0..15；-1 不覆盖
     */
    public record FluidRender(@Nullable String model, int brightness) {
    }

    public static final class Builder {
        private final Map<Key, FluidRender> fluids = new LinkedHashMap<>();
        private float[] bounds = {1 / 16f, 1 / 16f, 1 / 16f, 15 / 16f, 15 / 16f, 15 / 16f};
        private int levels = 16;
        private int transitionTicks = 5;
        private int transitionDelayTicks = 2;
        private float viewRange = 1f;
        private int brightness = -1;
        private Vector3f translation = new Vector3f();
        private Vector3f scale = new Vector3f(1, 1, 1);
        private Vector3f rotation = new Vector3f();

        private Builder() {
        }

        /**
         * 液体盒子，像素 0..16。
         */
        public Builder bounds(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
            return this.bounds(new float[]{minX, minY, minZ, maxX, maxY, maxZ});
        }

        private Builder bounds(float[] pixels) {
            this.bounds = new float[6];
            for (int i = 0; i < 6; i++) this.bounds[i] = pixels[i] / 16f;
            return this;
        }

        public Builder levels(int value) {
            this.levels = value;
            return this;
        }

        public Builder transitionTicks(int value) {
            this.transitionTicks = value;
            return this;
        }

        public Builder transitionDelayTicks(int value) {
            this.transitionDelayTicks = value;
            return this;
        }

        public Builder viewRange(float value) {
            this.viewRange = value;
            return this;
        }

        public Builder brightness(int value) {
            this.brightness = value;
            return this;
        }

        public Builder translation(Vector3f value) {
            this.translation = new Vector3f(Objects.requireNonNull(value, "translation"));
            return this;
        }

        public Builder scale(Vector3f value) {
            this.scale = new Vector3f(Objects.requireNonNull(value, "scale"));
            return this;
        }

        public Builder rotation(Vector3f value) {
            this.rotation = new Vector3f(Objects.requireNonNull(value, "rotation"));
            return this;
        }

        public Builder fluid(Key fluid, FluidRender render) {
            this.fluids.put(Objects.requireNonNull(fluid, "fluid"), Objects.requireNonNull(render, "render"));
            return this;
        }

        public TankRenderSpec build() {
            return new TankRenderSpec(this);
        }
    }
}
