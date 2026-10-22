package net.nyana.fluid.api.block;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.nyana.fluid.api.fluid.FluidAmount;
import net.nyana.fluid.api.persist.FluidDataFormat;
import net.nyana.fluid.api.recipe.FluidIngredient;
import net.nyana.fluid.api.render.TankRenderSpec;
import net.nyana.fluid.api.storage.base.SideConfig;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 储罐规格：代码里用 {@link #builder()}，或由 {@link FluidTankBehavior#factory()} 注册的类型从方块配置读
 * （{@code capacity}、{@code slots}、{@code sides}、{@code filter}、{@code keep_on_break}、{@code data_key}）。
 */
public final class TankSpec {
    /**
     * 新建 builder：1 个槽、各面双向、破坏保留、默认 {@code data_key}。
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 解析 CE 配置节，键见类说明。
     *
     * @throws IllegalArgumentException 配置不合法
     */
    public static TankSpec parse(ConfigSection section) {
        Builder builder = TankSpec.builder();
        Object capacity = section.get("capacity");
        if (capacity == null) throw new IllegalArgumentException("A tank needs a capacity");
        builder.capacity(capacity instanceof Number number ? number.longValue() : FluidAmount.parse(String.valueOf(capacity)));
        builder.slots(section.getInt("slots", 1));
        if (section.get("sides") instanceof Map<?, ?> sides) {
            java.util.LinkedHashMap<String, Object> map = new java.util.LinkedHashMap<>();
            sides.forEach((key, value) -> map.put(String.valueOf(key), value));
            builder.sides(SideConfig.parse(map));
        }
        Object filter = section.get("filter");
        if (filter != null) builder.filter(FluidIngredient.parse(filter));
        builder.keepOnBreak(section.getBoolean("keep_on_break", true));
        builder.dataKey(section.getString("data_key", FluidDataFormat.ROOT_KEY));
        builder.sharedCapacity(section.getBoolean("shared_capacity", false));
        if (section.get("renderer") instanceof Map<?, ?> renderer) builder.renderer(TankRenderSpec.parse(renderer));
        return builder.build();
    }

    private final long capacity;
    private final int slots;
    private final SideConfig sides;
    private final @Nullable FluidIngredient filter;
    private final boolean keepOnBreak;
    private final String dataKey;
    private final boolean sharedCapacity;
    private final @Nullable TankRenderSpec renderer;

    private TankSpec(Builder builder) {
        if (builder.capacity <= 0) throw new IllegalArgumentException("Tank capacity must be positive");
        if (builder.slots < 1 || builder.slots > FluidDataFormat.MAX_SLOTS) {
            throw new IllegalArgumentException("Tank slots must be within [1, " + FluidDataFormat.MAX_SLOTS + "]");
        }
        if (builder.dataKey.isBlank()) throw new IllegalArgumentException("data_key must not be blank");
        this.capacity = builder.capacity;
        this.slots = builder.slots;
        this.sides = builder.sides;
        this.filter = builder.filter;
        this.keepOnBreak = builder.keepOnBreak;
        this.dataKey = builder.dataKey;
        this.sharedCapacity = builder.sharedCapacity;
        this.renderer = builder.renderer;
    }

    public long capacity() {
        return this.capacity;
    }

    public int slots() {
        return this.slots;
    }

    public SideConfig sides() {
        return this.sides;
    }

    public Optional<FluidIngredient> filter() {
        return Optional.ofNullable(this.filter);
    }

    /**
     * 破坏时是否保留流体。
     */
    public boolean keepOnBreak() {
        return this.keepOnBreak;
    }

    /**
     * 数据在方块实体自定义数据里的键。
     */
    public String dataKey() {
        return this.dataKey;
    }

    /**
     * {@code true} 时 {@link #capacity()} 是所有槽共用的总容量，每种流体占一个槽。
     */
    public boolean sharedCapacity() {
        return this.sharedCapacity;
    }

    /**
     * 储罐里的流体展示实体；没有配置时不展示。
     */
    public Optional<TankRenderSpec> renderer() {
        return Optional.ofNullable(this.renderer);
    }

    @Override
    public String toString() {
        return "TankSpec[capacity=" + this.capacity + ", slots=" + this.slots + ", dataKey=" + this.dataKey + "]";
    }

    public static final class Builder {
        private long capacity;
        private int slots = 1;
        private SideConfig sides = SideConfig.both();
        private @Nullable FluidIngredient filter;
        private boolean keepOnBreak = true;
        private String dataKey = FluidDataFormat.ROOT_KEY;
        private boolean sharedCapacity;
        private @Nullable TankRenderSpec renderer;

        private Builder() {
        }

        public Builder capacity(long value) {
            this.capacity = value;
            return this;
        }

        public Builder slots(int value) {
            this.slots = value;
            return this;
        }

        public Builder sides(SideConfig value) {
            this.sides = Objects.requireNonNull(value, "sides");
            return this;
        }

        public Builder filter(@Nullable FluidIngredient value) {
            this.filter = value;
            return this;
        }

        public Builder keepOnBreak(boolean value) {
            this.keepOnBreak = value;
            return this;
        }

        public Builder sharedCapacity(boolean value) {
            this.sharedCapacity = value;
            return this;
        }

        public Builder renderer(@Nullable TankRenderSpec value) {
            this.renderer = value;
            return this;
        }

        public Builder dataKey(String value) {
            this.dataKey = Objects.requireNonNull(value, "dataKey");
            return this;
        }

        /**
         * @throws IllegalArgumentException 有值不合法
         */
        public TankSpec build() {
            return new TankSpec(this);
        }
    }
}
