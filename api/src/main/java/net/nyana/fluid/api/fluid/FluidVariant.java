package net.nyana.fluid.api.fluid;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.util.Key;
import net.nyana.fluid.api.NyanaFluid;
import net.nyana.fluid.api.fluid.component.FluidComponentType;
import net.nyana.fluid.api.fluid.component.FluidComponents;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 流体身份：key 加组件, 不含数量,.属性每次从注册表读, 未注册时是 {@link FluidAttributes#UNKNOWN},.
 */
public final class FluidVariant {
    private static final Key BLANK_KEY = Key.of("minecraft", "empty");
    private static final FluidVariant BLANK = new FluidVariant(FluidVariant.BLANK_KEY, FluidComponents.EMPTY);
    private static final Map<Key, FluidVariant> SIMPLE = new ConcurrentHashMap<>();

    /**
     * 空变体, 代表「没有流体」,.空槽的 {@code variant()} 返回它,.
     */
    public static FluidVariant blank() {
        return FluidVariant.BLANK;
    }

    public static FluidVariant of(Key fluid) {
        Objects.requireNonNull(fluid, "fluid");
        if (fluid.equals(FluidVariant.BLANK_KEY)) return FluidVariant.BLANK;
        return FluidVariant.SIMPLE.computeIfAbsent(fluid, key -> new FluidVariant(key, FluidComponents.EMPTY));
    }

    /**
     * 带组件的变体,.
     *
     * @throws IllegalArgumentException 给空变体加组件
     */
    public static FluidVariant of(Key fluid, FluidComponents components) {
        Objects.requireNonNull(components, "components");
        if (components.isEmpty()) return FluidVariant.of(fluid);
        if (Objects.requireNonNull(fluid, "fluid").equals(FluidVariant.BLANK_KEY))
            throw new IllegalArgumentException("The blank variant cannot carry components");
        return new FluidVariant(fluid, components);
    }

    /**
     * 某种流体的默认变体,.
     */
    public static FluidVariant of(Fluid fluid) {
        return FluidVariant.of(fluid.key());
    }

    private final Key fluid;
    private final FluidComponents components;
    private final int hash;

    private FluidVariant(Key fluid, FluidComponents components) {
        this.fluid = fluid;
        this.components = components;
        this.hash = 31 * fluid.hashCode() + components.hashCode();
    }

    /**
     * 流体 key,.空变体返回 {@code minecraft:empty},.
     */
    public Key fluidKey() {
        return this.fluid;
    }

    public FluidComponents components() {
        return this.components;
    }

    public boolean isBlank() {
        return this == FluidVariant.BLANK;
    }

    public boolean hasComponents() {
        return !this.components.isEmpty();
    }

    /**
     * 流体 key 是否等于给定值（忽略组件）,.
     */
    public boolean isOf(Key fluid) {
        return this.fluid.equals(fluid);
    }

    /**
     * 按当前注册表判断是否属于某个标签,.
     */
    public boolean is(Key tag) {
        return !this.isBlank() && NyanaFluid.fluids().tags().is(this.fluid, tag);
    }

    /**
     * 是否是同一种流体（忽略组件）,.
     */
    public boolean sameFluid(FluidVariant other) {
        return other != null && this.fluid.equals(other.fluid);
    }

    /**
     * 从当前注册表取流体定义,.
     */
    public Optional<Fluid> fluid() {
        return this.isBlank() ? Optional.empty() : NyanaFluid.fluids().get(this.fluid);
    }

    /**
     * 当前注册表里的属性,.
     */
    public FluidAttributes attributes() {
        return this.fluid().map(Fluid::attributes).orElse(FluidAttributes.UNKNOWN);
    }

    /**
     * 显示名：属性里配置的显示名, 没有就用流体 key,.
     */
    public Component displayName() {
        return this.attributes().displayName().orElseGet(() -> Component.text(this.fluid.toString()));
    }

    public <T> Optional<T> get(FluidComponentType<T> type) {
        return this.components.get(type);
    }

    /**
     * 返回带上某个组件后的新变体,.
     *
     * @throws IllegalStateException 对空变体调用
     */
    public <T> FluidVariant with(FluidComponentType<T> type, T value) {
        if (this.isBlank()) throw new IllegalStateException("The blank variant cannot carry components");
        return FluidVariant.of(this.fluid, this.components.with(type, value));
    }

    /**
     * 返回去掉某个组件后的新变体,.
     */
    public FluidVariant without(FluidComponentType<?> type) {
        FluidComponents next = this.components.without(type.key());
        return next == this.components ? this : FluidVariant.of(this.fluid, next);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        return other instanceof FluidVariant variant && variant.hash == this.hash && variant.fluid.equals(this.fluid)
                && variant.components.equals(this.components);
    }

    @Override
    public int hashCode() {
        return this.hash;
    }

    @Override
    public String toString() {
        if (this.isBlank()) return "FluidVariant[blank]";
        return this.components.isEmpty() ? "FluidVariant[" + this.fluid + "]" : "FluidVariant[" + this.fluid + " " + this.components + "]";
    }
}
