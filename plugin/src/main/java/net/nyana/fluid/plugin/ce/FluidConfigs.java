package net.nyana.fluid.plugin.ce;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.Key;
import net.nyana.fluid.api.fluid.FluidAmount;
import net.nyana.fluid.api.fluid.FluidAttributes;
import net.nyana.fluid.api.fluid.FluidRarity;
import net.nyana.fluid.api.fluid.FluidSoundEvent;
import net.nyana.fluid.api.item.BucketSpec;

import java.util.*;

final class FluidConfigs {
    static FluidAttributes attributes(ConfigSection section) {
        FluidAttributes.Builder builder = FluidAttributes.builder();
        String name = section.getString("display_name", (String) null);
        if (name != null) builder.displayName(MiniMessage.miniMessage().deserialize(name));
        Object color = section.get("color");
        if (color != null) builder.color(FluidConfigs.parseColor(color));
        builder.density(section.getInt("density", 1000));
        builder.viscosity(section.getInt("viscosity", 1000));
        builder.temperature(section.getInt("temperature", 300));
        builder.luminance(section.getInt("luminance", 0));
        builder.gaseous(section.getBoolean("gaseous", false));
        String rarity = section.getString("rarity", (String) null);
        if (rarity != null) builder.rarity(FluidRarity.valueOf(rarity.trim().toUpperCase(Locale.ROOT)));
        String displayItem = section.getString("display_item", (String) null);
        if (displayItem != null) builder.displayItem(Key.of(displayItem));
        String texture = section.getString("texture", (String) null);
        if (texture != null) builder.texture(Key.of(texture));
        builder.tinted(section.getBoolean("tint", true));
        if (section.get("sounds") instanceof Map<?, ?> sounds) {
            sounds.forEach((event, sound) -> builder.sound(FluidSoundEvent.of(FluidConfigs.eventKey(String.valueOf(event))), Key.of(String.valueOf(sound))));
        }
        return builder.build();
    }

    static List<BucketSpec> containers(Key fluid, ConfigSection section) {
        List<BucketSpec> specs = new ArrayList<>(2);
        String bucket = section.getString("bucket", (String) null);
        if (bucket != null)
            specs.add(FluidConfigs.container(fluid, Key.of("minecraft", "bucket"), Key.of(bucket), FluidAmount.BUCKET));
        String bottle = section.getString("bottle", (String) null);
        if (bottle != null)
            specs.add(FluidConfigs.container(fluid, Key.of("minecraft", "glass_bottle"), Key.of(bottle), FluidAmount.BOTTLE));
        return specs;
    }

    private static BucketSpec container(Key fluid, Key empty, Key full, long amount) {
        return BucketSpec.builder().emptyItem(empty).fullItem(full).fluid(fluid).amount(amount).build();
    }

    static List<Key> keys(Object value) {
        List<Key> keys = new ArrayList<>();
        if (value instanceof Collection<?> collection) {
            for (Object entry : collection) keys.add(Key.of(String.valueOf(entry)));
        } else if (value != null) {
            keys.add(Key.of(String.valueOf(value)));
        }
        return keys;
    }

    private static Key eventKey(String name) {
        return name.indexOf(':') >= 0 ? Key.of(name) : Key.of("nyana-fluid", name.toLowerCase(Locale.ROOT));
    }

    static int parseColor(Object value) {
        if (value instanceof Number number) return number.intValue();
        String text = String.valueOf(value).trim();
        if (text.startsWith("#")) text = text.substring(1);
        if (text.length() == 6) text = "FF" + text;
        if (text.length() != 8) throw new IllegalArgumentException("Color must be #RRGGBB or #AARRGGBB: " + value);
        return (int) Long.parseLong(text, 16);
    }

    private FluidConfigs() {
    }
}
