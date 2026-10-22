package net.nyana.fluid.api.internal;

import net.momirealms.craftengine.core.item.Item;
import net.nyana.fluid.api.fluid.FluidVariant;
import net.nyana.fluid.api.item.BucketSpec;
import net.nyana.fluid.api.item.ItemAccess;
import net.nyana.fluid.api.storage.FluidSlotView;
import net.nyana.fluid.api.storage.FluidStorage;
import net.nyana.fluid.api.transaction.TransactionContext;
import org.jetbrains.annotations.ApiStatus;

import java.util.Iterator;
import java.util.List;

/**
 * Storages for bucket-like items: a full item can only be emptied whole, an empty item only filled whole.
 */
@ApiStatus.Internal
public final class BucketItemStorage {
    private BucketItemStorage() {
    }

    /**
     * A full container: exactly {@code spec.amount()} of one fluid, extractable only as a whole.
     */
    static final class Full implements FluidStorage, FluidSlotView {
        private final ItemAccess access;
        private final BucketSpec spec;

        Full(ItemAccess access, BucketSpec spec) {
            this.access = access;
            this.spec = spec;
        }

        private boolean present() {
            return this.access.amount() > 0 && this.spec.matchesFull(this.access.item());
        }

        @Override
        public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            Preconditions.checkResource(resource, maxAmount);
            return 0;
        }

        @Override
        public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            Preconditions.checkResource(resource, maxAmount);
            if (maxAmount < this.spec.amount() || !resource.equals(this.spec.fluid()) || !this.present()) return 0;
            Item empty = this.spec.createEmpty();
            if (empty == null) return 0;
            return this.access.exchange(empty, 1, transaction) == 1 ? this.spec.amount() : 0;
        }

        @Override
        public Iterator<FluidSlotView> iterator() {
            return List.<FluidSlotView>of(this).iterator();
        }

        @Override
        public boolean supportsInsertion() {
            return false;
        }

        @Override
        public FluidVariant variant() {
            return this.present() ? this.spec.fluid() : FluidVariant.blank();
        }

        @Override
        public long amount() {
            return this.present() ? this.spec.amount() : 0;
        }

        @Override
        public long capacity() {
            return this.spec.amount();
        }
    }

    /**
     * An empty container that can become any of several full containers, filled only as a whole.
     */
    static final class Empty implements FluidStorage, FluidSlotView {
        private final ItemAccess access;
        private final List<BucketSpec> specs;

        Empty(ItemAccess access, List<BucketSpec> specs) {
            this.access = access;
            this.specs = specs;
        }

        @Override
        public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            Preconditions.checkResource(resource, maxAmount);
            if (this.access.amount() == 0) return 0;
            Item current = this.access.item();
            for (BucketSpec spec : this.specs) {
                if (!spec.fluid().equals(resource) || maxAmount < spec.amount() || !spec.matchesEmpty(current))
                    continue;
                Item full = spec.createFull();
                if (full == null) continue;
                if (this.access.exchange(full, 1, transaction) == 1) return spec.amount();
            }
            return 0;
        }

        @Override
        public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            Preconditions.checkResource(resource, maxAmount);
            return 0;
        }

        @Override
        public Iterator<FluidSlotView> iterator() {
            return List.<FluidSlotView>of(this).iterator();
        }

        @Override
        public boolean supportsExtraction() {
            return false;
        }

        @Override
        public FluidVariant variant() {
            return FluidVariant.blank();
        }

        @Override
        public long amount() {
            return 0;
        }

        @Override
        public long capacity() {
            long capacity = 0;
            for (BucketSpec spec : this.specs) capacity = Math.max(capacity, spec.amount());
            return capacity;
        }
    }
}
