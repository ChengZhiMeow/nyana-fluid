package net.nyana.fluid.plugin.render;

import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public record GaugeSpec(@Nullable Type type, @Nullable Integer amount, @Nullable Key texture,
                        @Nullable Map<Integer, Entry> items,
                        @Nullable Direction direction, @Nullable Key parent, @Nullable Key background,
                        @Nullable Key overlay,
                        @Nullable Boolean tint, @Nullable Map<?, ?> display, @Nullable Boolean oversizedInGui) {
    public static final Key DEFAULT_PARENT = Key.of("minecraft", "item/generated");

    public static GaugeSpec parse(Map<?, ?> values) {
        Map<Integer, Entry> items = null;
        if (values.get("items") instanceof Map<?, ?> map) {
            items = new TreeMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                int level = Integer.parseInt(String.valueOf(entry.getKey()).trim());
                if (level < 0) throw new IllegalArgumentException("Gauge item levels start at 0: " + level);
                items.put(level, Entry.parse(entry.getValue()));
            }
        }
        return new GaugeSpec(values.get("type") == null ? null : Type.valueOf(GaugeSpec.upper(values.get("type"))),
                values.get("amount") instanceof Number number ? number.intValue() : null, GaugeSpec.key(values.get("texture")), items,
                values.get("direction") == null ? null : Direction.valueOf(GaugeSpec.upper(values.get("direction"))),
                GaugeSpec.key(values.get("parent")), GaugeSpec.key(values.get("background")), GaugeSpec.key(values.get("overlay")),
                values.get("tint") instanceof Boolean flag ? flag : null,
                values.get("display") instanceof Map<?, ?> display ? display : null,
                values.get("oversized_in_gui") instanceof Boolean flag ? flag : null);
    }

    private static String upper(Object value) {
        return String.valueOf(value).trim().toUpperCase(Locale.ROOT);
    }

    private static <T> T or(@Nullable T value, @Nullable T fallback) {
        return value != null ? value : fallback;
    }

    private static @Nullable Key key(@Nullable Object value) {
        return value == null ? null : Key.of(String.valueOf(value));
    }

    public GaugeSpec over(@Nullable GaugeSpec base) {
        if (base == null) return this;
        Type type = this.type != null ? this.type : this.items != null ? Type.SELECT : this.texture != null ? Type.SPLIT : base.typeOrDefault();
        boolean same = type == base.typeOrDefault();
        return new GaugeSpec(type, GaugeSpec.or(this.amount, same ? base.amount : null), GaugeSpec.or(this.texture, same ? base.texture : null),
                GaugeSpec.or(this.items, same ? base.items : null), GaugeSpec.or(this.direction, same ? base.direction : null),
                GaugeSpec.or(this.parent, base.parent), GaugeSpec.or(this.background, base.background), GaugeSpec.or(this.overlay, base.overlay),
                GaugeSpec.or(this.tint, base.tint), GaugeSpec.or(this.display, base.display), GaugeSpec.or(this.oversizedInGui, base.oversizedInGui));
    }

    public Type typeOrDefault() {
        return this.type != null ? this.type : this.items != null ? Type.SELECT : Type.SPLIT;
    }

    public Direction directionOrDefault() {
        return this.direction == null ? Direction.UP : this.direction;
    }

    public Key parentOrDefault() {
        return this.parent == null ? GaugeSpec.DEFAULT_PARENT : this.parent;
    }

    public boolean tinted() {
        return this.tint == null || this.tint;
    }

    public boolean oversized() {
        return this.oversizedInGui != null && this.oversizedInGui;
    }

    public enum Type {
        SPLIT, SELECT
    }

    public enum Direction {
        UP, DOWN, LEFT, RIGHT;

        boolean horizontal() {
            return this == Direction.LEFT || this == Direction.RIGHT;
        }
    }

    public enum EntryType {
        TEXTURE, MODEL, ITEM
    }

    public record Entry(EntryType type, Key id) {
        static Entry parse(Object value) {
            if (value instanceof Map<?, ?> map) {
                EntryType type = map.get("type") == null ? EntryType.TEXTURE : EntryType.valueOf(GaugeSpec.upper(map.get("type")));
                Object id = map.get("id");
                if (id == null) throw new IllegalArgumentException("Gauge item needs an id");
                return new Entry(type, Key.of(String.valueOf(id)));
            }
            return new Entry(EntryType.TEXTURE, Key.of(String.valueOf(value)));
        }
    }
}
