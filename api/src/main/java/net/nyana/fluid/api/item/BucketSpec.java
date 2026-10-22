package net.nyana.fluid.api.item;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.Key;
import net.nyana.fluid.api.fluid.FluidVariant;
import net.nyana.fluid.api.internal.ItemStacks;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * 桶型容器：空物品和满物品互相交换，每次装入或倒出固定数量。例如空桶 ↔ 水桶（1000 mB）、玻璃瓶 ↔ 水瓶（250 mB）。
 *
 * <p>满物品默认按物品 id 识别和创建；像水瓶这种需要额外数据的物品，可以用
 * {@link Builder#fullItem(Key, Supplier, Predicate)} 自定义创建和识别方式。final class + builder，以后可以加字段。
 */
public final class BucketSpec {
    /**
     * 新建 builder。
     */
    public static Builder builder() {
        return new Builder();
    }

    private final Key emptyItem;
    private final Key fullItem;
    private final FluidVariant fluid;
    private final long amount;
    private final @Nullable Supplier<Item> fullFactory;
    private final @Nullable Predicate<Item> fullMatcher;
    private final @Nullable Key fillSound;
    private final @Nullable Key emptySound;

    private BucketSpec(Builder builder) {
        this.emptyItem = Objects.requireNonNull(builder.emptyItem, "emptyItem");
        this.fullItem = Objects.requireNonNull(builder.fullItem, "fullItem");
        this.fluid = Objects.requireNonNull(builder.fluid, "fluid");
        if (this.fluid.isBlank()) throw new IllegalArgumentException("A bucket must hold a fluid");
        if (builder.amount <= 0) throw new IllegalArgumentException("Amount must be positive");
        this.amount = builder.amount;
        this.fullFactory = builder.fullFactory;
        this.fullMatcher = builder.fullMatcher;
        this.fillSound = builder.fillSound;
        this.emptySound = builder.emptySound;
    }

    public Key emptyItem() {
        return this.emptyItem;
    }

    public Key fullItem() {
        return this.fullItem;
    }

    public FluidVariant fluid() {
        return this.fluid;
    }

    /**
     * 每次装入或倒出的数量。
     */
    public long amount() {
        return this.amount;
    }

    /**
     * 装入时的音效；没有配置时由交互逻辑选默认音效。
     */
    public Optional<Key> fillSound() {
        return Optional.ofNullable(this.fillSound);
    }

    public Optional<Key> emptySound() {
        return Optional.ofNullable(this.emptySound);
    }

    /**
     * 物品是不是这个规格的空物品。
     */
    public boolean matchesEmpty(Item item) {
        return !item.isEmpty() && item.id().equals(this.emptyItem);
    }

    /**
     * 物品是不是这个规格的满物品。
     */
    public boolean matchesFull(Item item) {
        return !item.isEmpty() && item.id().equals(this.fullItem) && (this.fullMatcher == null || this.fullMatcher.test(item));
    }

    /**
     * 创建一个空物品。需要 CE 正在运行。
     */
    public @Nullable Item createEmpty() {
        return ItemStacks.create(this.emptyItem);
    }

    /**
     * 创建一个满物品。需要 CE 正在运行。
     */
    public @Nullable Item createFull() {
        return this.fullFactory != null ? this.fullFactory.get() : ItemStacks.create(this.fullItem);
    }

    @Override
    public String toString() {
        return "BucketSpec[" + this.emptyItem + " <-> " + this.fullItem + ", " + this.fluid + " x " + this.amount + "]";
    }

    public static final class Builder {
        private @Nullable Key emptyItem;
        private @Nullable Key fullItem;
        private @Nullable FluidVariant fluid;
        private long amount;
        private @Nullable Supplier<Item> fullFactory;
        private @Nullable Predicate<Item> fullMatcher;
        private @Nullable Key fillSound;
        private @Nullable Key emptySound;

        private Builder() {
        }

        public Builder emptyItem(Key id) {
            this.emptyItem = Objects.requireNonNull(id, "id");
            return this;
        }

        /**
         * 设置满物品，按 id 识别和创建。
         */
        public Builder fullItem(Key id) {
            this.fullItem = Objects.requireNonNull(id, "id");
            this.fullFactory = null;
            this.fullMatcher = null;
            return this;
        }

        /**
         * 设置满物品，自定义创建和识别方式（例如水瓶需要药水数据）。
         */
        public Builder fullItem(Key id, Supplier<Item> factory, Predicate<Item> matcher) {
            this.fullItem = Objects.requireNonNull(id, "id");
            this.fullFactory = Objects.requireNonNull(factory, "factory");
            this.fullMatcher = Objects.requireNonNull(matcher, "matcher");
            return this;
        }

        public Builder fluid(FluidVariant fluid) {
            this.fluid = Objects.requireNonNull(fluid, "fluid");
            return this;
        }

        public Builder fluid(Key fluid) {
            return this.fluid(FluidVariant.of(fluid));
        }

        public Builder amount(long amount) {
            this.amount = amount;
            return this;
        }

        public Builder fillSound(@Nullable Key sound) {
            this.fillSound = sound;
            return this;
        }

        public Builder emptySound(@Nullable Key sound) {
            this.emptySound = sound;
            return this;
        }

        /**
         * @throws NullPointerException     缺少物品或流体
         * @throws IllegalArgumentException 数量不大于 0
         */
        public BucketSpec build() {
            return new BucketSpec(this);
        }
    }
}
