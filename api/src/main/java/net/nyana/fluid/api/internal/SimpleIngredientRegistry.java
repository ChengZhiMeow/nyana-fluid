package net.nyana.fluid.api.internal;

import net.momirealms.craftengine.core.util.Key;
import net.nyana.fluid.api.Registration;
import net.nyana.fluid.api.recipe.FluidIngredientRegistry;
import net.nyana.fluid.api.recipe.FluidIngredientType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ingredient kinds by key; the built-in kinds are always present.
 */
@ApiStatus.Internal
public final class SimpleIngredientRegistry implements FluidIngredientRegistry {
    private final Registrations registrations;
    private final ConcurrentHashMap<Key, FluidIngredientType<?>> types = new ConcurrentHashMap<>();

    public SimpleIngredientRegistry(Registrations registrations) {
        this.registrations = Objects.requireNonNull(registrations, "registrations");
        for (FluidIngredientType<?> type : Ingredients.builtins()) this.types.put(type.key(), type);
    }

    @Override
    public Registration register(Plugin owner, FluidIngredientType<?> type) {
        Objects.requireNonNull(type, "type");
        FluidIngredientType<?> existing = this.types.putIfAbsent(type.key(), type);
        if (existing != null && existing != type)
            throw new IllegalStateException("Ingredient type " + type.key() + " is already registered");
        return this.registrations.register(owner, () -> this.types.remove(type.key(), type));
    }

    @Override
    public Optional<FluidIngredientType<?>> get(Key key) {
        return Optional.ofNullable(this.types.get(key));
    }
}
