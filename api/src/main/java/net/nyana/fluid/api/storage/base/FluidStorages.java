package net.nyana.fluid.api.storage.base;

import net.momirealms.craftengine.core.util.Direction;
import net.nyana.fluid.api.fluid.FluidVariant;
import net.nyana.fluid.api.internal.Preconditions;
import net.nyana.fluid.api.recipe.FluidIngredient;
import net.nyana.fluid.api.storage.FluidSlotView;
import net.nyana.fluid.api.storage.FluidStorage;
import net.nyana.fluid.api.transaction.SnapshotJournal;
import net.nyana.fluid.api.transaction.TransactionContext;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

/**
 * 存储装饰器。装饰器可以叠加，例如 {@code rateLimited(filtered(tank, ingredient), 100, 100)}。
 * 装饰器不会绕过被装饰存储自己的访问校验。
 */
public final class FluidStorages {
    private static final FluidStorage EMPTY = new Empty();
    private static final FluidStorage VOIDING = new Voiding();

    /**
     * 永远为空、什么都不接受的存储。
     */
    public static FluidStorage empty() {
        return FluidStorages.EMPTY;
    }

    /**
     * 只读视图：能看到内容，但插入和抽取都返回 0。
     */
    public static FluidStorage readOnly(FluidStorage storage) {
        return new Access(storage, false, false);
    }

    /**
     * 只允许插入的视图。
     */
    public static FluidStorage insertOnly(FluidStorage storage) {
        return new Access(storage, true, false);
    }

    /**
     * 只允许抽取的视图。
     */
    public static FluidStorage extractOnly(FluidStorage storage) {
        return new Access(storage, false, true);
    }

    /**
     * 过滤视图：只有满足条件的变体能进出。
     */
    public static FluidStorage filtered(FluidStorage storage, Predicate<FluidVariant> filter) {
        return new Filtered(storage, filter);
    }

    /**
     * 按配方原料过滤，例如 {@code #mypack:molten_metal}。
     */
    public static FluidStorage filtered(FluidStorage storage, FluidIngredient ingredient) {
        return new Filtered(storage, Objects.requireNonNull(ingredient, "ingredient"));
    }

    /**
     * 限速视图：每个服务器 tick 最多插入 {@code insertPerTick}、最多抽取 {@code extractPerTick}。
     * 预算登记进事务，回滚时预算一起回滚。要长期持有同一个实例，重新创建会重置预算。
     */
    public static FluidStorage rateLimited(FluidStorage storage, long insertPerTick, long extractPerTick) {
        Preconditions.checkAmount(insertPerTick);
        Preconditions.checkAmount(extractPerTick);
        return new RateLimited(storage, insertPerTick, extractPerTick);
    }

    /**
     * 按面包装：每个面按 {@link SideConfig} 得到一个只进 / 只出 / 双向的视图，配置为 {@link SideAccess#NONE} 的面不暴露。
     * 每个面的视图只创建一次。
     */
    public static SidedFluidStorage sided(FluidStorage storage, SideConfig sides) {
        Objects.requireNonNull(storage, "storage");
        Objects.requireNonNull(sides, "sides");
        Map<Direction, FluidStorage> views = new EnumMap<>(Direction.class);
        for (Direction direction : Direction.values()) {
            FluidStorage view = FluidStorages.view(storage, sides.access(direction));
            if (view != null) views.put(direction, view);
        }
        FluidStorage internal = FluidStorages.view(storage, sides.access(null));
        return side -> side == null ? internal : views.get(side);
    }

    private static @Nullable FluidStorage view(FluidStorage storage, SideAccess access) {
        return switch (access) {
            case NONE -> null;
            case INPUT -> FluidStorages.insertOnly(storage);
            case OUTPUT -> FluidStorages.extractOnly(storage);
            default -> storage;
        };
    }

    /**
     * 无限源：可以无限抽出某种变体，不接受插入。适合创造模式储罐。
     */
    public static FluidStorage creative(FluidVariant resource) {
        Preconditions.checkResource(resource, 0);
        return new Creative(resource);
    }

    /**
     * 吞噬：接受任何插入并丢弃，不能抽取。适合垃圾桶。
     */
    public static FluidStorage voiding() {
        return FluidStorages.VOIDING;
    }

    private FluidStorages() {
    }

    private abstract static class Delegating implements FluidStorage {
        protected final FluidStorage delegate;

        Delegating(FluidStorage delegate) {
            this.delegate = Objects.requireNonNull(delegate, "delegate");
        }

        @Override
        public long version() {
            return this.delegate.version();
        }

        @Override
        public boolean isLocked() {
            return this.delegate.isLocked();
        }

        /**
         * Wraps each view of the delegate so extraction through the view goes through this decorator.
         */
        protected Iterator<FluidSlotView> wrapViews() {
            List<FluidSlotView> views = new ArrayList<>();
            for (FluidSlotView view : this.delegate) views.add(new View(view));
            return views.iterator();
        }

        /**
         * Extraction through a view; decorators override to apply their rules.
         */
        protected long extractFromView(FluidSlotView view, FluidVariant resource, long maxAmount, TransactionContext transaction) {
            return view.extract(resource, maxAmount, transaction);
        }

        private final class View implements FluidSlotView {
            private final FluidSlotView inner;

            private View(FluidSlotView inner) {
                this.inner = inner;
            }

            @Override
            public FluidVariant variant() {
                return this.inner.variant();
            }

            @Override
            public long amount() {
                return this.inner.amount();
            }

            @Override
            public long capacity() {
                return this.inner.capacity();
            }

            @Override
            public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
                Preconditions.checkResource(resource, maxAmount);
                return Delegating.this.extractFromView(this.inner, resource, maxAmount, transaction);
            }

            @Override
            public FluidSlotView underlying() {
                return this.inner.underlying();
            }
        }
    }

    private static final class Access extends Delegating {
        private final boolean insert;
        private final boolean extract;

        Access(FluidStorage delegate, boolean insert, boolean extract) {
            super(delegate);
            this.insert = insert;
            this.extract = extract;
        }

        @Override
        public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            Preconditions.checkResource(resource, maxAmount);
            return this.insert ? this.delegate.insert(resource, maxAmount, transaction) : 0;
        }

        @Override
        public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            Preconditions.checkResource(resource, maxAmount);
            return this.extract ? this.delegate.extract(resource, maxAmount, transaction) : 0;
        }

        @Override
        protected long extractFromView(FluidSlotView view, FluidVariant resource, long maxAmount, TransactionContext transaction) {
            return this.extract ? view.extract(resource, maxAmount, transaction) : 0;
        }

        @Override
        public Iterator<FluidSlotView> iterator() {
            return super.wrapViews();
        }

        @Override
        public boolean supportsInsertion() {
            return this.insert && this.delegate.supportsInsertion();
        }

        @Override
        public boolean supportsExtraction() {
            return this.extract && this.delegate.supportsExtraction();
        }
    }

    private static final class Filtered extends Delegating {
        private final Predicate<FluidVariant> filter;

        Filtered(FluidStorage delegate, Predicate<FluidVariant> filter) {
            super(delegate);
            this.filter = Objects.requireNonNull(filter, "filter");
        }

        @Override
        public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            Preconditions.checkResource(resource, maxAmount);
            return this.filter.test(resource) ? this.delegate.insert(resource, maxAmount, transaction) : 0;
        }

        @Override
        public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            Preconditions.checkResource(resource, maxAmount);
            return this.filter.test(resource) ? this.delegate.extract(resource, maxAmount, transaction) : 0;
        }

        @Override
        protected long extractFromView(FluidSlotView view, FluidVariant resource, long maxAmount, TransactionContext transaction) {
            return this.filter.test(resource) ? view.extract(resource, maxAmount, transaction) : 0;
        }

        @Override
        public Iterator<FluidSlotView> iterator() {
            return super.wrapViews();
        }

        @Override
        public boolean supportsInsertion() {
            return this.delegate.supportsInsertion();
        }

        @Override
        public boolean supportsExtraction() {
            return this.delegate.supportsExtraction();
        }
    }

    private static final class RateLimited extends Delegating {
        private final long insertRate;
        private final long extractRate;
        private final Budget budget = new Budget();

        RateLimited(FluidStorage delegate, long insertRate, long extractRate) {
            super(delegate);
            this.insertRate = insertRate;
            this.extractRate = extractRate;
        }

        @Override
        public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            Preconditions.checkResource(resource, maxAmount);
            long now = Bukkit.getCurrentTick();
            long allowed = Math.min(maxAmount, this.insertRate - this.budget.inserted(now));
            if (allowed <= 0) return 0;
            long inserted = Preconditions.checkResult(this.delegate.insert(resource, allowed, transaction), allowed);
            if (inserted > 0) this.budget.record(now, inserted, 0, transaction);
            return inserted;
        }

        @Override
        public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            return this.limitedExtract(resource, maxAmount, transaction, null);
        }

        @Override
        protected long extractFromView(FluidSlotView view, FluidVariant resource, long maxAmount, TransactionContext transaction) {
            return this.limitedExtract(resource, maxAmount, transaction, view);
        }

        private long limitedExtract(FluidVariant resource, long maxAmount, TransactionContext transaction, @Nullable FluidSlotView view) {
            Preconditions.checkResource(resource, maxAmount);
            long now = Bukkit.getCurrentTick();
            long allowed = Math.min(maxAmount, this.extractRate - this.budget.extracted(now));
            if (allowed <= 0) return 0;
            long extracted = view == null ? this.delegate.extract(resource, allowed, transaction) : view.extract(resource, allowed, transaction);
            Preconditions.checkResult(extracted, allowed);
            if (extracted > 0) this.budget.record(now, 0, extracted, transaction);
            return extracted;
        }

        @Override
        public Iterator<FluidSlotView> iterator() {
            return super.wrapViews();
        }

        @Override
        public boolean supportsInsertion() {
            return this.insertRate > 0 && this.delegate.supportsInsertion();
        }

        @Override
        public boolean supportsExtraction() {
            return this.extractRate > 0 && this.delegate.supportsExtraction();
        }

        /**
         * Per-tick budget, journaled so that a rolled-back transfer gives its budget back.
         */
        private static final class Budget extends SnapshotJournal<long[]> {
            private long tick = Long.MIN_VALUE;
            private long inserted;
            private long extracted;

            long inserted(long now) {
                return now == this.tick ? this.inserted : 0;
            }

            long extracted(long now) {
                return now == this.tick ? this.extracted : 0;
            }

            void record(long now, long insertedNow, long extractedNow, TransactionContext transaction) {
                super.updateSnapshots(transaction);
                if (now != this.tick) {
                    this.tick = now;
                    this.inserted = 0;
                    this.extracted = 0;
                }
                this.inserted = Math.addExact(this.inserted, insertedNow);
                this.extracted = Math.addExact(this.extracted, extractedNow);
            }

            @Override
            protected long[] createSnapshot() {
                return new long[]{this.tick, this.inserted, this.extracted};
            }

            @Override
            protected void revertToSnapshot(long[] snapshot) {
                this.tick = snapshot[0];
                this.inserted = snapshot[1];
                this.extracted = snapshot[2];
            }
        }
    }

    private static final class Creative implements FluidStorage {
        private final FluidVariant resource;
        private final FluidSlotView view;

        Creative(FluidVariant resource) {
            this.resource = resource;
            this.view = new FluidSlotView() {
                @Override
                public FluidVariant variant() {
                    return Creative.this.resource;
                }

                @Override
                public long amount() {
                    return Long.MAX_VALUE;
                }

                @Override
                public long capacity() {
                    return Long.MAX_VALUE;
                }

                @Override
                public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
                    return Creative.this.extract(resource, maxAmount, transaction);
                }
            };
        }

        @Override
        public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            Preconditions.checkResource(resource, maxAmount);
            return 0;
        }

        @Override
        public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            Preconditions.checkResource(resource, maxAmount);
            return resource.equals(this.resource) ? maxAmount : 0;
        }

        @Override
        public Iterator<FluidSlotView> iterator() {
            return List.of(this.view).iterator();
        }

        @Override
        public boolean supportsInsertion() {
            return false;
        }
    }

    private static final class Voiding implements FluidStorage {
        @Override
        public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            Preconditions.checkResource(resource, maxAmount);
            return maxAmount;
        }

        @Override
        public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            Preconditions.checkResource(resource, maxAmount);
            return 0;
        }

        @Override
        public Iterator<FluidSlotView> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public boolean supportsExtraction() {
            return false;
        }
    }

    private static final class Empty implements FluidStorage {
        @Override
        public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            Preconditions.checkResource(resource, maxAmount);
            return 0;
        }

        @Override
        public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            Preconditions.checkResource(resource, maxAmount);
            return 0;
        }

        @Override
        public Iterator<FluidSlotView> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public boolean supportsInsertion() {
            return false;
        }

        @Override
        public boolean supportsExtraction() {
            return false;
        }
    }
}
