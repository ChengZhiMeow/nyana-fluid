package net.nyana.fluid.api.fluid;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * 流体的元数据：显示名、物理量、颜色、音效,.只是数据, 库自己不用它们显示或改变任何东西,.
 * 单位：密度 kg/m³, 黏度 mPa·s, 温度 K, 颜色 ARGB,.
 */
public final class FluidAttributes {
    /**
     * 未注册流体的兜底属性,.
     */
    public static final FluidAttributes UNKNOWN = FluidAttributes.builder().build();

    /**
     * 新建一个 builder, 各项取默认值：密度 1000、黏度 1000、温度 300、亮度 0、普通稀有度、外观类别 {@code other},.
     */
    public static Builder builder() {
        return new Builder();
    }

    private final @Nullable Component displayName;
    private final int density;
    private final int viscosity;
    private final int temperature;
    private final int luminance;
    private final @Nullable Integer color;
    private final FluidRarity rarity;
    private final boolean gaseous;
    private final @Nullable Key displayItem;
    private final @Nullable Key texture;
    private final boolean tinted;
    private final Map<FluidSoundEvent, Key> sounds;

    private FluidAttributes(Builder builder) {
        if (builder.viscosity < 0) throw new IllegalArgumentException("Viscosity must not be negative");
        if (builder.temperature < 0) throw new IllegalArgumentException("Temperature must not be negative Kelvin");
        if (builder.luminance < 0 || builder.luminance > 15)
            throw new IllegalArgumentException("Luminance must be within [0, 15]");
        this.displayName = builder.displayName;
        this.density = builder.density;
        this.viscosity = builder.viscosity;
        this.temperature = builder.temperature;
        this.luminance = builder.luminance;
        this.color = builder.color;
        this.rarity = builder.rarity;
        this.gaseous = builder.gaseous;
        this.displayItem = builder.displayItem;
        this.texture = builder.texture;
        this.tinted = builder.tinted;
        this.sounds = Map.copyOf(builder.sounds);
    }

    /**
     * 以当前属性为起点新建一个 builder,.
     */
    public Builder toBuilder() {
        Builder builder = new Builder();
        builder.displayName = this.displayName;
        builder.density = this.density;
        builder.viscosity = this.viscosity;
        builder.temperature = this.temperature;
        builder.luminance = this.luminance;
        builder.color = this.color;
        builder.rarity = this.rarity;
        builder.gaseous = this.gaseous;
        builder.displayItem = this.displayItem;
        builder.texture = this.texture;
        builder.tinted = this.tinted;
        builder.sounds.putAll(this.sounds);
        return builder;
    }

    /**
     * 显示名,.没有设置时, 显示层会退回到流体的 key,.
     */
    public Optional<Component> displayName() {
        return Optional.ofNullable(this.displayName);
    }

    /**
     * 密度, kg/m³；负数表示比空气轻,.
     */
    public int density() {
        return this.density;
    }

    /**
     * 黏度, mPa·s,.
     */
    public int viscosity() {
        return this.viscosity;
    }

    public int temperature() {
        return this.temperature;
    }

    public int luminance() {
        return this.luminance;
    }

    /**
     * 颜色, 32 位 ARGB,.
     */
    public OptionalInt color() {
        return this.color == null ? OptionalInt.empty() : OptionalInt.of(this.color);
    }

    public FluidRarity rarity() {
        return this.rarity;
    }

    public boolean gaseous() {
        return this.gaseous;
    }

    /**
     * GUI、tooltip 里代表这种流体的图标物品（CE 物品 id 或原版物品 id）,.
     */
    public Optional<Key> displayItem() {
        return Optional.ofNullable(this.displayItem);
    }

    /**
     * 渲染用的流体贴图（储罐模型、菜单）,.没有设置时用原版 {@code minecraft:block/water_still},.
     */
    public Optional<Key> texture() {
        return Optional.ofNullable(this.texture);
    }

    /**
     * 贴图是否按 {@link #color()} 染色,.默认 {@code true},.
     */
    public boolean tinted() {
        return this.tinted;
    }

    public Optional<Key> sound(FluidSoundEvent event) {
        return Optional.ofNullable(this.sounds.get(Objects.requireNonNull(event, "event")));
    }

    /**
     * 全部已配置的音效,.
     */
    public Map<FluidSoundEvent, Key> sounds() {
        return this.sounds;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof FluidAttributes that)) return false;
        return this.density == that.density && this.viscosity == that.viscosity && this.temperature == that.temperature
                && this.luminance == that.luminance && this.gaseous == that.gaseous && Objects.equals(this.displayName, that.displayName)
                && Objects.equals(this.color, that.color) && this.rarity == that.rarity && Objects.equals(this.displayItem, that.displayItem)
                && Objects.equals(this.texture, that.texture) && this.tinted == that.tinted && this.sounds.equals(that.sounds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.displayName, this.density, this.viscosity, this.temperature, this.luminance, this.color,
                this.rarity, this.gaseous, this.displayItem, this.texture, this.tinted, this.sounds);
    }

    @Override
    public String toString() {
        return "FluidAttributes[density=" + this.density + ", viscosity=" + this.viscosity + ", temperature=" + this.temperature
                + ", luminance=" + this.luminance + ", gaseous=" + this.gaseous + "]";
    }

    /**
     * {@link FluidAttributes} 的构造器,.每个 setter 都返回自身, 最后调用 {@link #build()},.
     */
    public static final class Builder {
        private final Map<FluidSoundEvent, Key> sounds = new LinkedHashMap<>();
        private @Nullable Component displayName;
        private int density = 1000;
        private int viscosity = 1000;
        private int temperature = 300;
        private int luminance;
        private @Nullable Integer color;
        private FluidRarity rarity = FluidRarity.COMMON;
        private boolean gaseous;
        private @Nullable Key displayItem;
        private @Nullable Key texture;
        private boolean tinted = true;

        private Builder() {
        }

        public Builder displayName(@Nullable Component value) {
            this.displayName = value;
            return this;
        }

        public Builder density(int value) {
            this.density = value;
            return this;
        }

        public Builder viscosity(int value) {
            this.viscosity = value;
            return this;
        }

        public Builder temperature(int value) {
            this.temperature = value;
            return this;
        }

        public Builder luminance(int value) {
            this.luminance = value;
            return this;
        }

        public Builder color(@Nullable Integer argb) {
            this.color = argb;
            return this;
        }

        public Builder rarity(FluidRarity value) {
            this.rarity = Objects.requireNonNull(value, "rarity");
            return this;
        }

        public Builder gaseous(boolean value) {
            this.gaseous = value;
            return this;
        }

        public Builder displayItem(@Nullable Key value) {
            this.displayItem = value;
            return this;
        }

        public Builder texture(@Nullable Key value) {
            this.texture = value;
            return this;
        }

        public Builder tinted(boolean value) {
            this.tinted = value;
            return this;
        }

        public Builder sound(FluidSoundEvent event, @Nullable Key sound) {
            Objects.requireNonNull(event, "event");
            if (sound == null) this.sounds.remove(event);
            else this.sounds.put(event, sound);
            return this;
        }

        /**
         * 构造不可变的属性对象,.
         *
         * @throws IllegalArgumentException 有数值超出范围
         */
        public FluidAttributes build() {
            return new FluidAttributes(this);
        }
    }
}
