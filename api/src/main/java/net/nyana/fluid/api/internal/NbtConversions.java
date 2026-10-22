package net.nyana.fluid.api.internal;

import net.momirealms.craftengine.libraries.nbt.*;
import org.jetbrains.annotations.ApiStatus;

import java.util.Collection;
import java.util.Map;

/**
 * Converts plain YAML / Java config values into NBT tags (for component values written in configuration).
 */
@ApiStatus.Internal
public final class NbtConversions {
    public static Tag toTag(Object value) {
        if (value instanceof Tag tag) return tag.deepClone();
        if (value instanceof Integer number) return IntTag.valueOf(number);
        if (value instanceof Long number) return LongTag.valueOf(number);
        if (value instanceof Short number) return ShortTag.valueOf(number);
        if (value instanceof Byte number) return ByteTag.valueOf(number);
        if (value instanceof Double number) return new DoubleTag(number);
        if (value instanceof Float number) return new FloatTag(number);
        if (value instanceof Boolean flag) return ByteTag.valueOf(flag);
        if (value instanceof String text) return new StringTag(text);
        if (value instanceof Map<?, ?> map) {
            CompoundTag compound = new CompoundTag();
            map.forEach((key, entry) -> compound.put(String.valueOf(key), NbtConversions.toTag(entry)));
            return compound;
        }
        if (value instanceof Collection<?> collection) {
            ListTag list = new ListTag();
            for (Object entry : collection) list.add(list.size(), NbtConversions.toTag(entry));
            return list;
        }
        throw new IllegalArgumentException("Cannot convert " + (value == null ? "null" : value.getClass().getSimpleName()) + " to NBT");
    }

    private NbtConversions() {
    }
}
