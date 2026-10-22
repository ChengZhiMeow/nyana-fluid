package net.nyana.fluid.api.internal;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemDefinition;
import net.nyana.fluid.api.Registration;
import net.nyana.fluid.api.item.BucketSpec;
import net.nyana.fluid.api.item.ContainerSpec;
import net.nyana.fluid.api.item.FluidContainers;
import net.nyana.fluid.api.item.ItemAccess;
import net.nyana.fluid.api.lookup.FluidLookups;
import net.nyana.fluid.api.storage.FluidStorage;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Bucket-like containers. Each spec registers an item lookup provider for its two item ids; whichever provider
 * answers builds a storage from <em>all</em> specs currently registered for that item, so the answer is the same.
 */
@ApiStatus.Internal
public final class SimpleFluidContainers implements FluidContainers {
    private final Registrations registrations;
    private final List<BucketSpec> buckets = new CopyOnWriteArrayList<>();

    public SimpleFluidContainers(Registrations registrations) {
        this.registrations = Objects.requireNonNull(registrations, "registrations");
    }

    @Override
    public Registration registerBucket(Plugin owner, BucketSpec spec) {
        Objects.requireNonNull(spec, "spec");
        this.buckets.add(spec);
        Registration lookup = FluidLookups.ITEM.registerForItems(owner, this::storageFor, spec.emptyItem(), spec.fullItem());
        return this.registrations.register(owner, () -> {
            this.buckets.remove(spec);
            lookup.close();
        });
    }

    @Override
    public List<BucketSpec> buckets() {
        return List.copyOf(this.buckets);
    }

    @Override
    public Optional<ContainerSpec> containerSpec(Item item) {
        if (item == null || item.isEmpty()) return Optional.empty();
        return item.getDefinition().map(ItemDefinition::settings).map(settings -> settings.getCustomData(ContainerSpec.SETTING));
    }

    private @Nullable FluidStorage storageFor(Item item, @Nullable ItemAccess access) {
        ItemAccess location = access != null ? access : ItemAccess.detached(item);
        for (BucketSpec spec : this.buckets) {
            if (spec.matchesFull(item)) return new BucketItemStorage.Full(location, spec);
        }
        List<BucketSpec> empties = new ArrayList<>();
        for (BucketSpec spec : this.buckets) if (spec.matchesEmpty(item)) empties.add(spec);
        return empties.isEmpty() ? null : new BucketItemStorage.Empty(location, List.copyOf(empties));
    }
}
