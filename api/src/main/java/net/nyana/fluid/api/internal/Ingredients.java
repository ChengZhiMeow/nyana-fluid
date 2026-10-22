package net.nyana.fluid.api.internal;

import net.momirealms.craftengine.core.util.Key;
import net.nyana.fluid.api.NyanaFluid;
import net.nyana.fluid.api.fluid.FluidVariant;
import net.nyana.fluid.api.fluid.component.FluidComponents;
import net.nyana.fluid.api.recipe.FluidIngredient;
import net.nyana.fluid.api.recipe.FluidIngredientType;
import org.jetbrains.annotations.ApiStatus;

import java.util.*;

/**
 * Built-in ingredient kinds and the configuration parser.
 */
@ApiStatus.Internal
public final class Ingredients {
    public static final FluidIngredientType<Single> FLUID = FluidIngredientType.of(Key.of("nyana-fluid", "fluid"),
            config -> new Single(Key.of(Ingredients.required(config, "fluid"))));
    public static final FluidIngredientType<TagIngredient> TAG = FluidIngredientType.of(Key.of("nyana-fluid", "tag"),
            config -> new TagIngredient(Key.of(Ingredients.stripHash(Ingredients.required(config, "tag")))));
    public static final FluidIngredientType<AnyOf> ANY_OF = FluidIngredientType.of(Key.of("nyana-fluid", "any_of"),
            config -> new AnyOf(Ingredients.parseList(config.get("options"))));
    public static final FluidIngredientType<WithComponents> COMPONENTS = FluidIngredientType.of(Key.of("nyana-fluid", "components"),
            config -> new WithComponents(Ingredients.parse(config.get("base")), Ingredients.components(config.get("components"))));
    public static final FluidIngredientType<Empty> EMPTY_TYPE = FluidIngredientType.of(Key.of("nyana-fluid", "empty"), config -> Empty.INSTANCE);
    public static final FluidIngredient EMPTY = Empty.INSTANCE;

    public static List<FluidIngredientType<?>> builtins() {
        return List.of(Ingredients.FLUID, Ingredients.TAG, Ingredients.ANY_OF, Ingredients.COMPONENTS, Ingredients.EMPTY_TYPE);
    }

    public static FluidIngredient parse(Object value) {
        if (value instanceof FluidIngredient ingredient) return ingredient;
        if (value instanceof String text) {
            String trimmed = text.trim();
            if (trimmed.isEmpty()) throw new IllegalArgumentException("Empty fluid ingredient");
            return trimmed.startsWith("#") ? new TagIngredient(Key.of(trimmed.substring(1))) : new Single(Key.of(trimmed));
        }
        if (value instanceof Collection<?> collection) return new AnyOf(Ingredients.parseList(collection));
        if (value instanceof Map<?, ?> raw) {
            Map<String, Object> map = new LinkedHashMap<>();
            raw.forEach((key, entry) -> map.put(String.valueOf(key), entry));
            if (map.containsKey("type")) {
                Key typeKey = Key.of(String.valueOf(map.get("type")));
                FluidIngredientType<?> type = NyanaFluid.ingredients().get(typeKey)
                        .orElseThrow(() -> new IllegalArgumentException("Unknown fluid ingredient type: " + typeKey));
                return type.parser().parse(map);
            }
            FluidIngredient base;
            if (map.containsKey("fluid")) base = new Single(Key.of(String.valueOf(map.get("fluid"))));
            else if (map.containsKey("tag"))
                base = new TagIngredient(Key.of(Ingredients.stripHash(String.valueOf(map.get("tag")))));
            else throw new IllegalArgumentException("A fluid ingredient map needs 'type', 'fluid' or 'tag'");
            return map.containsKey("components") ? new WithComponents(base, Ingredients.components(map.get("components"))) : base;
        }
        throw new IllegalArgumentException("Not a fluid ingredient: " + value);
    }

    private static List<FluidIngredient> parseList(Object value) {
        if (!(value instanceof Collection<?> collection))
            throw new IllegalArgumentException("Expected a list of fluid ingredients");
        List<FluidIngredient> options = new ArrayList<>(collection.size());
        for (Object entry : collection) options.add(Ingredients.parse(entry));
        return List.copyOf(options);
    }

    private static FluidComponents components(Object value) {
        if (!(value instanceof Map<?, ?> map)) throw new IllegalArgumentException("components must be a map");
        FluidComponents.Builder builder = FluidComponents.builder();
        map.forEach((key, entry) -> builder.putRaw(Key.of(String.valueOf(key)), NbtConversions.toTag(entry)));
        return builder.build();
    }

    private static String required(Map<String, Object> config, String key) {
        Object value = config.get(key);
        if (value == null) throw new IllegalArgumentException("Missing '" + key + "'");
        return String.valueOf(value);
    }

    private static String stripHash(String text) {
        return text.startsWith("#") ? text.substring(1) : text;
    }

    private Ingredients() {
    }

    public enum Empty implements FluidIngredient {
        INSTANCE;

        @Override
        public FluidIngredientType<?> type() {
            return Ingredients.EMPTY_TYPE;
        }

        @Override
        public boolean test(FluidVariant variant) {
            return false;
        }

        @Override
        public List<FluidVariant> displayVariants() {
            return List.of();
        }
    }

    public record Single(Key fluid) implements FluidIngredient {
        public Single {
            Objects.requireNonNull(fluid, "fluid");
        }

        @Override
        public FluidIngredientType<?> type() {
            return Ingredients.FLUID;
        }

        @Override
        public boolean test(FluidVariant variant) {
            return variant != null && !variant.isBlank() && variant.isOf(this.fluid);
        }

        @Override
        public List<FluidVariant> displayVariants() {
            return List.of(FluidVariant.of(this.fluid));
        }
    }

    public record TagIngredient(Key tag) implements FluidIngredient {
        public TagIngredient {
            Objects.requireNonNull(tag, "tag");
        }

        @Override
        public FluidIngredientType<?> type() {
            return Ingredients.TAG;
        }

        @Override
        public boolean test(FluidVariant variant) {
            return variant != null && variant.is(this.tag);
        }

        @Override
        public List<FluidVariant> displayVariants() {
            List<FluidVariant> variants = new ArrayList<>();
            for (Key member : NyanaFluid.fluids().tags().members(this.tag)) variants.add(FluidVariant.of(member));
            return List.copyOf(variants);
        }
    }

    public record AnyOf(List<FluidIngredient> options) implements FluidIngredient {
        public AnyOf {
            options = List.copyOf(options);
        }

        @Override
        public FluidIngredientType<?> type() {
            return Ingredients.ANY_OF;
        }

        @Override
        public boolean test(FluidVariant variant) {
            for (FluidIngredient option : this.options) if (option.test(variant)) return true;
            return false;
        }

        @Override
        public List<FluidVariant> displayVariants() {
            Set<FluidVariant> variants = new LinkedHashSet<>();
            for (FluidIngredient option : this.options) variants.addAll(option.displayVariants());
            return List.copyOf(variants);
        }
    }

    public record WithComponents(FluidIngredient base, FluidComponents required) implements FluidIngredient {
        public WithComponents {
            Objects.requireNonNull(base, "base");
            Objects.requireNonNull(required, "required");
        }

        @Override
        public FluidIngredientType<?> type() {
            return Ingredients.COMPONENTS;
        }

        @Override
        public boolean test(FluidVariant variant) {
            if (!this.base.test(variant)) return false;
            for (Key key : this.required.keys()) {
                if (!variant.components().raw(key).equals(this.required.raw(key))) return false;
            }
            return true;
        }

        @Override
        public List<FluidVariant> displayVariants() {
            List<FluidVariant> variants = new ArrayList<>();
            for (FluidVariant variant : this.base.displayVariants()) {
                FluidComponents.Builder builder = variant.components().toBuilder();
                for (Key key : this.required.keys()) builder.putRaw(key, this.required.raw(key).orElseThrow());
                variants.add(FluidVariant.of(variant.fluidKey(), builder.build()));
            }
            return List.copyOf(variants);
        }
    }
}
