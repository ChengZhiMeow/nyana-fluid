package net.nyana.fluid.api.storage.base;

import net.momirealms.craftengine.core.util.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * 每个面的进出规则, 以及「内部 / 不分面」（查找上下文为 {@code null} 时）的规则,.
 */
public final class SideConfig {
    private static final SideConfig ALL_BOTH = SideConfig.all(SideAccess.BOTH);

    /**
     * 所有面（包括内部）都用同一个规则,.
     */
    public static SideConfig all(SideAccess access) {
        Objects.requireNonNull(access, "access");
        Map<Direction, SideAccess> map = new EnumMap<>(Direction.class);
        for (Direction direction : Direction.values()) map.put(direction, access);
        return new SideConfig(map, access);
    }

    /**
     * 所有面都允许进出,.
     */
    public static SideConfig both() {
        return SideConfig.ALL_BOTH;
    }

    /**
     * 新建 builder, 各面默认 {@link SideAccess#BOTH},.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 解析配置,.键可以是 {@code up}、{@code down}、{@code north}、{@code south}、{@code west}、{@code east}、
     * {@code horizontal}、{@code vertical}、{@code all}、{@code internal}, 后写的覆盖先写的；
     * 值是 {@code none} / {@code input} / {@code output} / {@code both},.
     *
     * @throws IllegalArgumentException 不认识的键或值
     */
    public static SideConfig parse(Map<String, ?> map) {
        Builder builder = SideConfig.builder();
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            SideAccess access = SideAccess.parse(String.valueOf(entry.getValue()));
            switch (entry.getKey().trim().toLowerCase(Locale.ROOT)) {
                case "up" -> builder.up(access);
                case "down" -> builder.down(access);
                case "north" -> builder.side(Direction.NORTH, access);
                case "south" -> builder.side(Direction.SOUTH, access);
                case "west" -> builder.side(Direction.WEST, access);
                case "east" -> builder.side(Direction.EAST, access);
                case "horizontal" -> builder.horizontal(access);
                case "vertical" -> builder.vertical(access);
                case "all" -> builder.all(access);
                case "internal" -> builder.internal(access);
                default -> throw new IllegalArgumentException("Unknown side: " + entry.getKey());
            }
        }
        return builder.build();
    }

    private final Map<Direction, SideAccess> sides;
    private final SideAccess internal;

    private SideConfig(Map<Direction, SideAccess> sides, SideAccess internal) {
        this.sides = new EnumMap<>(sides);
        this.internal = internal;
    }

    public SideAccess access(@Nullable Direction side) {
        return side == null ? this.internal : this.sides.getOrDefault(side, SideAccess.NONE);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof SideConfig config && config.sides.equals(this.sides) && config.internal == this.internal;
    }

    @Override
    public int hashCode() {
        return 31 * this.sides.hashCode() + this.internal.hashCode();
    }

    @Override
    public String toString() {
        return "SideConfig" + this.sides + "[internal=" + this.internal + "]";
    }

    public static final class Builder {
        private final Map<Direction, SideAccess> sides = new EnumMap<>(Direction.class);
        private SideAccess internal = SideAccess.BOTH;

        private Builder() {
            for (Direction direction : Direction.values()) this.sides.put(direction, SideAccess.BOTH);
        }

        public Builder side(Direction side, SideAccess access) {
            this.sides.put(Objects.requireNonNull(side, "side"), Objects.requireNonNull(access, "access"));
            return this;
        }

        public Builder up(SideAccess access) {
            return this.side(Direction.UP, access);
        }

        public Builder down(SideAccess access) {
            return this.side(Direction.DOWN, access);
        }

        public Builder horizontal(SideAccess access) {
            this.side(Direction.NORTH, access);
            this.side(Direction.SOUTH, access);
            this.side(Direction.WEST, access);
            return this.side(Direction.EAST, access);
        }

        public Builder vertical(SideAccess access) {
            this.up(access);
            return this.down(access);
        }

        public Builder all(SideAccess access) {
            this.horizontal(access);
            return this.vertical(access);
        }

        /**
         * 设置内部 / 不分面（查找上下文为 {@code null}, 例如 GUI）的规则, 默认 {@link SideAccess#BOTH},.
         */
        public Builder internal(SideAccess access) {
            this.internal = Objects.requireNonNull(access, "access");
            return this;
        }

        public SideConfig build() {
            return new SideConfig(this.sides, this.internal);
        }
    }
}
