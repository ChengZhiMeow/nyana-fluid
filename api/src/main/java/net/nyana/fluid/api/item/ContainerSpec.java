package net.nyana.fluid.api.item;

import net.momirealms.craftengine.core.item.setting.CustomItemSettingType;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.nyana.fluid.api.fluid.FluidAmount;
import net.nyana.fluid.api.recipe.FluidIngredient;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

/**
 * 数据型容器规格，CE 物品设置 {@code nyana-fluid:container}：{@code capacity}、{@code slots}、{@code filter}。
 */
public final class ContainerSpec {
    /**
     * CE 物品设置的类型。NyanaFluid 插件把它注册为 {@code nyana-fluid:container}。
     */
    public static final CustomItemSettingType<ContainerSpec> SETTING = CustomItemSettingType.newType(null, (spec, sink) -> {
    });

    /**
     * 新建 builder：1 个槽、不过滤。
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 解析 CE 配置节，键见类说明。
     *
     * @throws IllegalArgumentException 配置不合法
     */
    public static ContainerSpec parse(ConfigSection section) {
        Builder builder = ContainerSpec.builder();
        Object capacity = section.get("capacity");
        if (capacity == null) throw new IllegalArgumentException("nyana-fluid:container needs a capacity");
        builder.capacity(capacity instanceof Number number ? number.longValue() : FluidAmount.parse(String.valueOf(capacity)));
        builder.slots(section.getInt("slots", 1));
        Object filter = section.get("filter");
        if (filter != null) builder.filter(FluidIngredient.parse(filter));
        return builder.build();
    }

    private final long capacity;
    private final int slots;
    private final @Nullable FluidIngredient filter;

    private ContainerSpec(Builder builder) {
        if (builder.capacity <= 0) throw new IllegalArgumentException("Container capacity must be positive");
        if (builder.slots < 1) throw new IllegalArgumentException("A container needs at least one slot");
        this.capacity = builder.capacity;
        this.slots = builder.slots;
        this.filter = builder.filter;
    }

    public long capacity() {
        return this.capacity;
    }

    public int slots() {
        return this.slots;
    }

    public Optional<FluidIngredient> filter() {
        return Optional.ofNullable(this.filter);
    }

    @Override
    public String toString() {
        return "ContainerSpec[capacity=" + this.capacity + ", slots=" + this.slots + ", filter=" + this.filter + "]";
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ContainerSpec spec && spec.capacity == this.capacity && spec.slots == this.slots
                && Objects.equals(spec.filter, this.filter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.capacity, this.slots, this.filter);
    }

    public static final class Builder {
        private long capacity;
        private int slots = 1;
        private @Nullable FluidIngredient filter;

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

        public Builder filter(@Nullable FluidIngredient value) {
            this.filter = value;
            return this;
        }

        /**
         * @throws IllegalArgumentException 容量或槽数不合法
         */
        public ContainerSpec build() {
            return new ContainerSpec(this);
        }
    }
}
