package net.nyana.fluid.api.internal;

import net.momirealms.craftengine.core.util.Key;
import net.nyana.fluid.api.Registration;
import net.nyana.fluid.api.fluid.*;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Copy-on-write fluid registry. Writes are serialized on {@link #lock}; reads go to the volatile snapshot.
 * Listeners are notified outside the lock, after the new snapshot is visible.
 */
@ApiStatus.Internal
public final class SimpleFluidRegistry implements FluidRegistry {
    private static Map<Key, Set<Key>> normalize(Map<Key, Set<Key>> tags) {
        TreeMap<String, Map.Entry<Key, Set<Key>>> sorted = new TreeMap<>();
        tags.forEach((tag, members) -> {
            if (!members.isEmpty()) sorted.put(tag.toString(), Map.entry(tag, Set.copyOf(members)));
        });
        Map<Key, Set<Key>> result = new LinkedHashMap<>();
        for (Map.Entry<Key, Set<Key>> entry : sorted.values()) result.put(entry.getKey(), entry.getValue());
        return Collections.unmodifiableMap(result);
    }

    private final Registrations registrations;
    private final Object lock = new Object();
    private final Map<Key, CodeFluid> code = new LinkedHashMap<>();
    private final List<CodeTag> codeTags = new ArrayList<>();
    private final List<Consumer<FluidRegistryChange>> listeners = new CopyOnWriteArrayList<>();
    private final Tags tags = new Tags();
    private Map<Key, FluidAttributes> config = Map.of();
    private Map<Key, Set<Key>> configTags = Map.of();
    private volatile FluidRegistrySnapshot snapshot = new FluidRegistrySnapshot(0, Map.of(), Map.of());
    private volatile Map<Key, Set<Key>> tagsByFluid = Map.of();

    public SimpleFluidRegistry(Registrations registrations) {
        this.registrations = Objects.requireNonNull(registrations, "registrations");
    }

    /**
     * Called with every non-empty change, outside the registry lock.
     */
    public void addListener(Consumer<FluidRegistryChange> listener) {
        this.listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    /**
     * Replaces everything that came from CE configuration (fluids and tags) in one snapshot.
     */
    public void replaceConfig(Map<Key, FluidAttributes> fluids, Map<Key, Set<Key>> tags) {
        FluidRegistryChange change;
        synchronized (this.lock) {
            this.config = Map.copyOf(fluids);
            Map<Key, Set<Key>> copy = new HashMap<>();
            tags.forEach((tag, members) -> copy.put(tag, Set.copyOf(members)));
            this.configTags = Map.copyOf(copy);
            change = this.rebuild(FluidRegistryChange.Cause.CONFIG_RELOAD);
        }
        this.notifyListeners(change);
    }

    /**
     * Keys that configuration declared but code registration overrides; the plugin logs these as warnings.
     */
    public Set<Key> overriddenConfigKeys() {
        synchronized (this.lock) {
            Set<Key> overridden = new LinkedHashSet<>(this.config.keySet());
            overridden.retainAll(this.code.keySet());
            return Set.copyOf(overridden);
        }
    }

    @Override
    public Optional<Fluid> get(Key key) {
        return this.snapshot.get(Objects.requireNonNull(key, "key"));
    }

    @Override
    public Fluid getOrThrow(Key key) {
        return this.get(key).orElseThrow(() -> new NoSuchElementException("Fluid is not registered: " + key));
    }

    @Override
    public boolean contains(Key key) {
        return this.snapshot.fluids().containsKey(key);
    }

    @Override
    public Collection<Fluid> all() {
        return this.snapshot.fluids().values();
    }

    @Override
    public Set<Key> keys() {
        return this.snapshot.fluids().keySet();
    }

    @Override
    public Collection<Fluid> byOwner(Plugin owner) {
        List<Fluid> result = new ArrayList<>();
        for (Fluid fluid : this.snapshot.fluids().values()) {
            if (fluid.source() == Fluid.Source.CODE && fluid.owner().filter(owner::equals).isPresent())
                result.add(fluid);
        }
        return List.copyOf(result);
    }

    @Override
    public FluidRegistrySnapshot snapshot() {
        return this.snapshot;
    }

    @Override
    public long generation() {
        return this.snapshot.generation();
    }

    @Override
    public FluidTags tags() {
        return this.tags;
    }

    @Override
    public Registration register(Plugin owner, Key key, FluidAttributes attributes) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(attributes, "attributes");
        CodeFluid entry;
        FluidRegistryChange change;
        synchronized (this.lock) {
            CodeFluid existing = this.code.get(key);
            if (existing != null && existing.owner != owner) {
                throw new IllegalStateException("Fluid " + key + " is already registered by " + existing.owner.getName());
            }
            if (existing != null) existing.detach();
            entry = new CodeFluid(owner, key, attributes);
            this.code.put(key, entry);
            change = this.rebuild(FluidRegistryChange.Cause.API);
        }
        this.notifyListeners(change);
        return entry.handle;
    }

    @Override
    public boolean update(Plugin owner, Key key, FluidAttributes attributes) {
        Objects.requireNonNull(attributes, "attributes");
        FluidRegistryChange change;
        synchronized (this.lock) {
            CodeFluid existing = this.code.get(key);
            if (existing == null || existing.owner != owner) return false;
            existing.attributes = attributes;
            change = this.rebuild(FluidRegistryChange.Cause.API);
        }
        this.notifyListeners(change);
        return true;
    }

    @Override
    public boolean unregister(Plugin owner, Key key) {
        CodeFluid removed;
        synchronized (this.lock) {
            removed = this.code.get(key);
            if (removed == null || removed.owner != owner) return false;
        }
        this.removeEntry(removed);
        return true;
    }

    @Override
    public int unregisterAll(Plugin owner) {
        List<CodeFluid> owned = new ArrayList<>();
        FluidRegistryChange change;
        synchronized (this.lock) {
            for (CodeFluid entry : this.code.values()) if (entry.owner == owner) owned.add(entry);
            if (owned.isEmpty()) return 0;
            for (CodeFluid entry : owned) {
                this.code.remove(entry.key);
                entry.detach();
            }
            change = this.rebuild(FluidRegistryChange.Cause.API);
        }
        this.notifyListeners(change);
        return owned.size();
    }

    @Override
    public void batch(Plugin owner, Consumer<Batch> operations) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(operations, "operations");
        FluidRegistryChange change;
        synchronized (this.lock) {
            BatchImpl batch = new BatchImpl(owner, new LinkedHashMap<>(this.code));
            operations.accept(batch);
            // Only now does the batch take effect: detach replaced entries, then publish the staged map.
            for (CodeFluid previous : this.code.values()) {
                if (batch.staged.get(previous.key) != previous) previous.detach();
            }
            this.code.clear();
            this.code.putAll(batch.staged);
            for (CodeFluid entry : batch.created) entry.attach();
            change = this.rebuild(FluidRegistryChange.Cause.API);
        }
        this.notifyListeners(change);
    }

    private void removeEntry(CodeFluid entry) {
        FluidRegistryChange change;
        synchronized (this.lock) {
            if (this.code.get(entry.key) != entry) return;
            this.code.remove(entry.key);
            entry.detach();
            change = this.rebuild(FluidRegistryChange.Cause.API);
        }
        this.notifyListeners(change);
    }

    private void notifyListeners(@Nullable FluidRegistryChange change) {
        if (change == null) return;
        RuntimeException failure = null;
        for (Consumer<FluidRegistryChange> listener : this.listeners) {
            try {
                listener.accept(change);
            } catch (RuntimeException exception) {
                if (failure == null) failure = exception;
                else failure.addSuppressed(exception);
            }
        }
        if (failure != null) throw failure;
    }

    /**
     * Builds the next snapshot; returns the change, or {@code null} when nothing observable changed.
     */
    private @Nullable FluidRegistryChange rebuild(FluidRegistryChange.Cause cause) {
        Map<Key, Set<Key>> tagMembers = new HashMap<>();
        this.configTags.forEach((tag, members) -> tagMembers.computeIfAbsent(tag, ignored -> new HashSet<>()).addAll(members));
        for (CodeTag tag : this.codeTags)
            tagMembers.computeIfAbsent(tag.tag, ignored -> new HashSet<>()).addAll(tag.members);

        Map<Key, Set<Key>> byFluid = new HashMap<>();
        tagMembers.forEach((tag, members) -> {
            for (Key member : members) byFluid.computeIfAbsent(member, ignored -> new HashSet<>()).add(tag);
        });

        TreeMap<String, Fluid> sorted = new TreeMap<>();
        this.config.forEach((key, attributes) -> sorted.put(key.toString(),
                new Fluid(key, attributes, Fluid.Source.CONFIG, null, byFluid.getOrDefault(key, Set.of()))));
        for (CodeFluid entry : this.code.values()) {
            sorted.put(entry.key.toString(), new Fluid(entry.key, entry.attributes, Fluid.Source.CODE, entry.owner,
                    byFluid.getOrDefault(entry.key, Set.of())));
        }
        Map<Key, Fluid> fluids = new LinkedHashMap<>();
        for (Fluid fluid : sorted.values()) fluids.put(fluid.key(), fluid);

        FluidRegistrySnapshot previous = this.snapshot;
        Set<Key> added = new LinkedHashSet<>(fluids.keySet());
        added.removeAll(previous.fluids().keySet());
        Set<Key> removed = new LinkedHashSet<>(previous.fluids().keySet());
        removed.removeAll(fluids.keySet());
        Set<Key> updated = new LinkedHashSet<>();
        for (Map.Entry<Key, Fluid> entry : fluids.entrySet()) {
            Fluid before = previous.fluids().get(entry.getKey());
            if (before != null && !before.equals(entry.getValue())) updated.add(entry.getKey());
        }
        boolean tagsChanged = !previous.tags().equals(SimpleFluidRegistry.normalize(tagMembers));
        if (added.isEmpty() && removed.isEmpty() && updated.isEmpty() && !tagsChanged) return null;

        long generation = previous.generation() + 1;
        Map<Key, Set<Key>> frozenByFluid = new HashMap<>();
        byFluid.forEach((fluid, set) -> frozenByFluid.put(fluid, Set.copyOf(set)));
        this.tagsByFluid = Map.copyOf(frozenByFluid);
        this.snapshot = new FluidRegistrySnapshot(generation, fluids, SimpleFluidRegistry.normalize(tagMembers));
        return new FluidRegistryChange(generation, added, removed, updated, cause);
    }

    private record CodeTag(Plugin owner, Key tag, Set<Key> members) {
    }

    private final class CodeFluid {
        private final Plugin owner;
        private final Key key;
        private volatile FluidAttributes attributes;
        private Registration handle;
        private volatile boolean detached;

        private CodeFluid(Plugin owner, Key key, FluidAttributes attributes) {
            this(owner, key, attributes, true);
        }

        private CodeFluid(Plugin owner, Key key, FluidAttributes attributes, boolean attachNow) {
            this.owner = owner;
            this.key = key;
            this.attributes = attributes;
            if (attachNow) this.attach();
        }

        private void attach() {
            if (this.handle == null) this.handle = SimpleFluidRegistry.this.registrations.register(this.owner, () -> {
                if (!this.detached) SimpleFluidRegistry.this.removeEntry(this);
            });
        }

        /**
         * Marks the entry as no longer in the map and quietly closes its registration handle.
         */
        private void detach() {
            this.detached = true;
            if (this.handle != null) this.handle.close();
        }
    }

    private final class BatchImpl implements Batch {
        private final Plugin owner;
        private final Map<Key, CodeFluid> staged;
        private final List<CodeFluid> created = new ArrayList<>();

        private BatchImpl(Plugin owner, Map<Key, CodeFluid> staged) {
            this.owner = owner;
            this.staged = staged;
        }

        @Override
        public Batch register(Key key, FluidAttributes attributes) {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(attributes, "attributes");
            CodeFluid existing = this.staged.get(key);
            if (existing != null && existing.owner != this.owner) {
                throw new IllegalStateException("Fluid " + key + " is already registered by " + existing.owner.getName());
            }
            CodeFluid entry = new CodeFluid(this.owner, key, attributes, false);
            this.staged.put(key, entry);
            this.created.add(entry);
            return this;
        }

        @Override
        public Batch update(Key key, FluidAttributes attributes) {
            Objects.requireNonNull(attributes, "attributes");
            CodeFluid existing = this.staged.get(key);
            if (existing != null && existing.owner == this.owner) {
                CodeFluid replacement = new CodeFluid(this.owner, key, attributes, false);
                this.staged.put(key, replacement);
                this.created.add(replacement);
            }
            return this;
        }

        @Override
        public Batch unregister(Key key) {
            CodeFluid existing = this.staged.get(key);
            if (existing != null && existing.owner == this.owner) {
                this.staged.remove(key);
                this.created.remove(existing);
            }
            return this;
        }
    }

    private final class Tags implements FluidTags {
        @Override
        public boolean is(Key fluid, Key tag) {
            Set<Key> set = SimpleFluidRegistry.this.tagsByFluid.get(fluid);
            return set != null && set.contains(tag);
        }

        @Override
        public Set<Key> members(Key tag) {
            return SimpleFluidRegistry.this.snapshot.tags().getOrDefault(tag, Set.of());
        }

        @Override
        public Set<Key> tagsOf(Key fluid) {
            return SimpleFluidRegistry.this.tagsByFluid.getOrDefault(fluid, Set.of());
        }

        @Override
        public Registration register(Plugin owner, Key tag, Collection<Key> fluids) {
            Objects.requireNonNull(owner, "owner");
            Objects.requireNonNull(tag, "tag");
            CodeTag entry = new CodeTag(owner, tag, Set.copyOf(fluids));
            FluidRegistryChange change;
            synchronized (SimpleFluidRegistry.this.lock) {
                SimpleFluidRegistry.this.codeTags.add(entry);
                change = SimpleFluidRegistry.this.rebuild(FluidRegistryChange.Cause.API);
            }
            SimpleFluidRegistry.this.notifyListeners(change);
            return SimpleFluidRegistry.this.registrations.register(owner, () -> {
                FluidRegistryChange removal;
                synchronized (SimpleFluidRegistry.this.lock) {
                    if (!SimpleFluidRegistry.this.codeTags.remove(entry)) return;
                    removal = SimpleFluidRegistry.this.rebuild(FluidRegistryChange.Cause.API);
                }
                SimpleFluidRegistry.this.notifyListeners(removal);
            });
        }
    }
}
