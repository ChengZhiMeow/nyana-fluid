package net.nyana.fluid.api.internal;

import net.momirealms.craftengine.core.util.Key;
import net.nyana.fluid.api.Registration;
import net.nyana.fluid.api.fluid.component.FluidComponentRegistry;
import net.nyana.fluid.api.fluid.component.FluidComponentType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Component types by key. Registering the same type object again is allowed and reference-counted per registration.
 */
@ApiStatus.Internal
public final class SimpleComponentRegistry implements FluidComponentRegistry {
    private final Registrations registrations;
    private final ConcurrentHashMap<Key, Holder> types = new ConcurrentHashMap<>();

    public SimpleComponentRegistry(Registrations registrations) {
        this.registrations = Objects.requireNonNull(registrations, "registrations");
    }

    @Override
    public Registration register(Plugin owner, FluidComponentType<?> type) {
        Objects.requireNonNull(type, "type");
        Holder holder = this.types.compute(type.key(), (key, existing) -> {
            if (existing == null) return new Holder(type);
            if (existing.type.codec() != type.codec()) {
                throw new IllegalStateException("Component " + key + " is already registered with a different codec");
            }
            existing.references++;
            return existing;
        });
        return this.registrations.register(owner, () -> this.types.computeIfPresent(type.key(), (key, existing) -> {
            if (existing != holder) return existing;
            return --existing.references <= 0 ? null : existing;
        }));
    }

    @Override
    public Optional<FluidComponentType<?>> get(Key key) {
        Holder holder = this.types.get(key);
        return holder == null ? Optional.empty() : Optional.of(holder.type);
    }

    @Override
    public Collection<FluidComponentType<?>> all() {
        return List.copyOf(this.types.values().stream().<FluidComponentType<?>>map(holder -> holder.type).toList());
    }

    private static final class Holder {
        private final FluidComponentType<?> type;
        private int references = 1;

        private Holder(FluidComponentType<?> type) {
            this.type = type;
        }
    }
}
