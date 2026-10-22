package net.nyana.fluid.api.internal;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * The results of one {@code BlockLookupCache}, per context. They stay valid while the block state, the block entity and
 * the lookup's registrations are the same; any change throws them all away. {@code null} results are cached too, so a
 * pipe polling an empty neighbour does not run every provider each tick. Not thread-safe: a cache belongs to one machine,
 * which runs on one thread (its region's).
 */
public final class CachedLookup<C, A> {
    private static final Object NULL_CONTEXT = new Object();
    private static final Object NULL_RESULT = new Object();
    private final Map<Object, Object> results = new HashMap<>(4);
    private boolean valid;
    private int state;
    private @Nullable Object blockEntity;
    private long generation;

    /**
     * The cached result for the context, or a fresh one from {@code lookup} when the key changed or it was never asked.
     */
    @SuppressWarnings("unchecked")
    public @Nullable A get(int state, @Nullable Object blockEntity, long generation, @Nullable C context, Function<@Nullable C, @Nullable A> lookup) {
        if (!this.valid || this.state != state || this.blockEntity != blockEntity || this.generation != generation) {
            this.results.clear();
            this.valid = true;
            this.state = state;
            this.blockEntity = blockEntity;
            this.generation = generation;
        }
        Object key = context == null ? CachedLookup.NULL_CONTEXT : context;
        Object cached = this.results.get(key);
        if (cached != null) return cached == CachedLookup.NULL_RESULT ? null : (A) cached;
        A found = lookup.apply(context);
        this.results.put(key, found == null ? CachedLookup.NULL_RESULT : found);
        return found;
    }

    /**
     * Forgets everything, e.g. when the chunk unloaded.
     */
    public void invalidate() {
        this.results.clear();
        this.valid = false;
        this.blockEntity = null;
    }
}
