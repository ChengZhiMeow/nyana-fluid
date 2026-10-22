package net.nyana.fluid.api.internal;

import net.nyana.fluid.api.Registration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Tracks every registration by owner so that a disabled plugin's registrations are all closed together.
 */
@ApiStatus.Internal
public final class Registrations {
    private final Map<Plugin, List<Entry>> byOwner = new ConcurrentHashMap<>();

    /**
     * Creates a registration that runs {@code onClose} once, either when closed or when its owner is disabled.
     */
    public Registration register(Plugin owner, Runnable onClose) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(onClose, "onClose");
        Entry entry = new Entry(owner, onClose);
        this.byOwner.computeIfAbsent(owner, ignored -> new CopyOnWriteArrayList<>()).add(entry);
        return entry;
    }

    /**
     * Closes everything the owner registered. Returns how many registrations were still active.
     */
    public int closeAll(Plugin owner) {
        List<Entry> entries = this.byOwner.remove(owner);
        if (entries == null) return 0;
        int closed = 0;
        RuntimeException failure = null;
        for (Entry entry : entries) {
            try {
                if (entry.closeQuietly()) closed++;
            } catch (RuntimeException exception) {
                if (failure == null) failure = exception;
                else failure.addSuppressed(exception);
            }
        }
        if (failure != null) throw failure;
        return closed;
    }

    private final class Entry implements Registration {
        private final Plugin owner;
        private final Runnable onClose;
        private final AtomicBoolean active = new AtomicBoolean(true);

        private Entry(Plugin owner, Runnable onClose) {
            this.owner = owner;
            this.onClose = onClose;
        }

        @Override
        public Plugin owner() {
            return this.owner;
        }

        @Override
        public boolean active() {
            return this.active.get();
        }

        @Override
        public void close() {
            if (!this.closeQuietly()) return;
            List<Entry> entries = Registrations.this.byOwner.get(this.owner);
            if (entries != null) entries.remove(this);
        }

        private boolean closeQuietly() {
            if (!this.active.compareAndSet(true, false)) return false;
            this.onClose.run();
            return true;
        }

        @Override
        public String toString() {
            return "Registration[owner=" + this.owner.getName() + ", active=" + this.active.get() + "]";
        }
    }
}
