package net.nyana.fluid.api.internal;

import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.core.block.entity.BlockEntity;
import net.momirealms.craftengine.core.block.entity.BlockEntityController;
import net.momirealms.craftengine.core.entity.Entity;
import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemDefinition;
import net.momirealms.craftengine.core.item.setting.CustomItemSettingType;
import net.momirealms.craftengine.core.item.setting.ItemSettings;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.CEWorld;
import net.momirealms.craftengine.core.world.ExistingBlock;
import net.momirealms.craftengine.core.world.World;
import net.nyana.fluid.api.Registration;
import net.nyana.fluid.api.lookup.*;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Lookup singletons and their implementations. Provider lists are copy-on-write; lookups never lock.
 */
@ApiStatus.Internal
public final class Lookups {
    private static final int MAX_CONTROLLER_ORDER = 32;
    private static final Map<Key, BlockLookupImpl<?, ?>> BLOCKS = new ConcurrentHashMap<>();
    private static final Map<Key, ItemLookupImpl<?, ?>> ITEMS = new ConcurrentHashMap<>();
    private static final Map<Key, EntityLookupImpl<?, ?>> ENTITIES = new ConcurrentHashMap<>();
    private static final Map<Key, FurnitureLookupImpl<?, ?>> FURNITURE = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public static <A, C> BlockLookup<A, C> block(Key id, Class<A> api, Class<C> context) {
        BlockLookupImpl<?, ?> lookup = Lookups.BLOCKS.computeIfAbsent(Objects.requireNonNull(id, "id"), key -> new BlockLookupImpl<>(key, api, context));
        Lookups.checkTypes(id, lookup.api, lookup.context, api, context);
        return (BlockLookup<A, C>) lookup;
    }

    @SuppressWarnings("unchecked")
    public static <A, C> ItemLookup<A, C> item(Key id, Class<A> api, Class<C> context) {
        ItemLookupImpl<?, ?> lookup = Lookups.ITEMS.computeIfAbsent(Objects.requireNonNull(id, "id"), key -> new ItemLookupImpl<>(key, api, context));
        Lookups.checkTypes(id, lookup.api, lookup.context, api, context);
        return (ItemLookup<A, C>) lookup;
    }

    @SuppressWarnings("unchecked")
    public static <A, C> EntityLookup<A, C> entity(Key id, Class<A> api, Class<C> context) {
        EntityLookupImpl<?, ?> lookup = Lookups.ENTITIES.computeIfAbsent(Objects.requireNonNull(id, "id"), key -> new EntityLookupImpl<>(key, api, context));
        Lookups.checkTypes(id, lookup.api, lookup.context, api, context);
        return (EntityLookup<A, C>) lookup;
    }

    @SuppressWarnings("unchecked")
    public static <A, C> FurnitureLookup<A, C> furniture(Key id, Class<A> api, Class<C> context) {
        FurnitureLookupImpl<?, ?> lookup = Lookups.FURNITURE.computeIfAbsent(Objects.requireNonNull(id, "id"), key -> new FurnitureLookupImpl<>(key, api, context));
        Lookups.checkTypes(id, lookup.api, lookup.context, api, context);
        return (FurnitureLookup<A, C>) lookup;
    }

    private static void checkTypes(Key id, Class<?> api, Class<?> context, Class<?> wantedApi, Class<?> wantedContext) {
        if (api != wantedApi || context != wantedContext) {
            throw new IllegalArgumentException("Lookup " + id + " already exists as <" + api.getSimpleName() + ", " + context.getSimpleName()
                    + ">, not <" + wantedApi.getSimpleName() + ", " + wantedContext.getSimpleName() + ">");
        }
    }

    private static Registration track(Plugin owner, Runnable onClose) {
        return Core.get().registrations.register(owner, onClose);
    }

    private static <T> Registration addTo(Plugin owner, List<T> list, T entry) {
        list.add(entry);
        return Lookups.track(owner, () -> list.remove(entry));
    }

    /**
     * The CE block entity at a custom block, or {@code null} for vanilla blocks and unloaded positions.
     */
    public static @Nullable BlockEntity blockEntityOf(ExistingBlock block) {
        if (!block.isCustom()) return null;
        CEWorld world = block.world().storageWorld();
        if (world == null) return null;
        return world.getBlockEntityAtIfLoaded(new BlockPos(block.x(), block.y(), block.z()));
    }

    /**
     * All controllers of a type on a block entity, in CE composite order.
     */
    public static <T> List<T> controllers(BlockEntityController root, Class<T> type) {
        List<T> found = new ArrayList<>(2);
        for (int order = 0; order < Lookups.MAX_CONTROLLER_ORDER; order++) {
            T controller = root.get(type, order);
            if (controller == null) break;
            found.add(controller);
        }
        return found;
    }

    private Lookups() {
    }

    private static final class BlockLookupImpl<A, C> implements BlockLookup<A, C> {
        private final Key id;
        private final Class<A> api;
        private final Class<C> context;
        private final List<ControllerEntry<?, A, C>> controllers = new CopyOnWriteArrayList<>();
        private final Map<Key, List<BlockProvider<A, C>>> byId = new ConcurrentHashMap<>();
        private final List<TagEntry<A, C>> byTag = new CopyOnWriteArrayList<>();
        private final List<BlockProvider<A, C>> fallbacks = new CopyOnWriteArrayList<>();
        /**
         * Bumped on every registration and unregistration, so caches drop results computed with other providers.
         */
        private final AtomicLong generation = new AtomicLong();

        private BlockLookupImpl(Key id, Class<A> api, Class<C> context) {
            this.id = id;
            this.api = api;
            this.context = context;
        }

        @Override
        public Key id() {
            return this.id;
        }

        @Override
        public Class<A> apiClass() {
            return this.api;
        }

        @Override
        public Class<C> contextClass() {
            return this.context;
        }

        @Override
        public @Nullable A find(ExistingBlock block, @Nullable C context) {
            Objects.requireNonNull(block, "block");
            BlockEntity blockEntity = Lookups.blockEntityOf(block);
            if (blockEntity != null && blockEntity.controller != null) {
                for (ControllerEntry<?, A, C> entry : this.controllers) {
                    A found = entry.find(blockEntity.controller, context);
                    if (found != null) return found;
                }
            }
            List<BlockProvider<A, C>> providers = this.byId.get(block.id());
            if (providers != null) {
                for (BlockProvider<A, C> provider : providers) {
                    A found = provider.find(block, blockEntity, context);
                    if (found != null) return found;
                }
            }
            for (TagEntry<A, C> entry : this.byTag) {
                if (!block.is(entry.tag)) continue;
                A found = entry.provider.find(block, blockEntity, context);
                if (found != null) return found;
            }
            for (BlockProvider<A, C> provider : this.fallbacks) {
                A found = provider.find(block, blockEntity, context);
                if (found != null) return found;
            }
            return null;
        }

        @Override
        public @Nullable A find(World world, BlockPos pos, @Nullable C context) {
            if (world.getChunkIfLoaded(pos.x() >> 4, pos.z() >> 4) == null) return null;
            return this.find(world.getBlock(pos), context);
        }

        @Override
        public @Nullable A find(org.bukkit.block.Block block, @Nullable C context) {
            if (!block.getWorld().isChunkLoaded(block.getX() >> 4, block.getZ() >> 4)) return null;
            return this.find(BukkitAdaptor.adapt(block), context);
        }

        @Override
        public Registration registerForBlocks(Plugin owner, BlockProvider<A, C> provider, Key... blockIds) {
            Objects.requireNonNull(provider, "provider");
            List<Key> ids = List.of(blockIds);
            for (Key blockId : ids)
                this.byId.computeIfAbsent(blockId, ignored -> new CopyOnWriteArrayList<>()).add(provider);
            this.generation.incrementAndGet();
            return Lookups.track(owner, () -> {
                for (Key blockId : ids) {
                    List<BlockProvider<A, C>> list = this.byId.get(blockId);
                    if (list != null) list.remove(provider);
                }
                this.generation.incrementAndGet();
            });
        }

        @Override
        public Registration registerForTag(Plugin owner, BlockProvider<A, C> provider, Key blockTag) {
            return this.add(owner, this.byTag, new TagEntry<>(Objects.requireNonNull(blockTag, "blockTag"), Objects.requireNonNull(provider, "provider")));
        }

        @Override
        public <T> Registration registerForController(Plugin owner, Class<T> controllerType, ControllerProvider<T, A, C> provider) {
            return this.add(owner, this.controllers, new ControllerEntry<>(Objects.requireNonNull(controllerType, "controllerType"),
                    Objects.requireNonNull(provider, "provider")));
        }

        @Override
        public Registration registerFallback(Plugin owner, BlockProvider<A, C> provider) {
            return this.add(owner, this.fallbacks, Objects.requireNonNull(provider, "provider"));
        }

        private <T> Registration add(Plugin owner, List<T> list, T entry) {
            list.add(entry);
            this.generation.incrementAndGet();
            return Lookups.track(owner, () -> {
                list.remove(entry);
                this.generation.incrementAndGet();
            });
        }

        @Override
        public BlockLookupCache<A, C> cache(World world, BlockPos pos) {
            Objects.requireNonNull(world, "world");
            Objects.requireNonNull(pos, "pos");
            BlockPos fixed = new BlockPos(pos.x(), pos.y(), pos.z());
            CachedLookup<C, A> cached = new CachedLookup<>();
            return new BlockLookupCache<>() {
                @Override
                public @Nullable A find(@Nullable C context) {
                    if (world.getChunkIfLoaded(fixed.x() >> 4, fixed.z() >> 4) == null) {
                        cached.invalidate();
                        return null;
                    }
                    ExistingBlock block = world.getBlock(fixed);
                    return cached.get(block.blockState().registryId(), Lookups.blockEntityOf(block), BlockLookupImpl.this.generation.get(), context,
                            ctx -> BlockLookupImpl.this.find(block, ctx));
                }

                @Override
                public World world() {
                    return world;
                }

                @Override
                public BlockPos pos() {
                    return fixed;
                }
            };
        }

        @Override
        public String toString() {
            return "BlockLookup[" + this.id + "]";
        }
    }

    private record ControllerEntry<T, A, C>(Class<T> type, ControllerProvider<T, A, C> provider) {
        @Nullable A find(BlockEntityController root, @Nullable C context) {
            List<T> found = Lookups.controllers(root, this.type);
            return found.isEmpty() ? null : this.provider.findAll(found, context);
        }
    }

    private record TagEntry<A, C>(Key tag, BlockProvider<A, C> provider) {
    }

    private static final class ItemLookupImpl<A, C> implements ItemLookup<A, C> {
        private final Key id;
        private final Class<A> api;
        private final Class<C> context;
        private final List<SettingEntry<?, A, C>> settings = new CopyOnWriteArrayList<>();
        private final Map<Key, List<ItemProvider<A, C>>> byId = new ConcurrentHashMap<>();
        private final List<ItemTagEntry<A, C>> byTag = new CopyOnWriteArrayList<>();
        private final List<ItemProvider<A, C>> fallbacks = new CopyOnWriteArrayList<>();

        private ItemLookupImpl(Key id, Class<A> api, Class<C> context) {
            this.id = id;
            this.api = api;
            this.context = context;
        }

        @Override
        public Key id() {
            return this.id;
        }

        @Override
        public @Nullable A find(Item item, @Nullable C context) {
            if (item == null || item.isEmpty()) return null;
            if (!this.settings.isEmpty()) {
                Optional<ItemDefinition> definition = item.getDefinition();
                if (definition.isPresent()) {
                    ItemSettings itemSettings = definition.get().settings();
                    for (SettingEntry<?, A, C> entry : this.settings) {
                        A found = entry.find(itemSettings, item, context);
                        if (found != null) return found;
                    }
                }
            }
            List<ItemProvider<A, C>> providers = this.byId.get(item.id());
            if (providers != null) {
                for (ItemProvider<A, C> provider : providers) {
                    A found = provider.find(item, context);
                    if (found != null) return found;
                }
            }
            for (ItemTagEntry<A, C> entry : this.byTag) {
                if (!item.hasVanillaTag(entry.tag) && !item.hasPluginTag(entry.tag)) continue;
                A found = entry.provider.find(item, context);
                if (found != null) return found;
            }
            for (ItemProvider<A, C> provider : this.fallbacks) {
                A found = provider.find(item, context);
                if (found != null) return found;
            }
            return null;
        }

        @Override
        public Registration registerForItems(Plugin owner, ItemProvider<A, C> provider, Key... itemIds) {
            Objects.requireNonNull(provider, "provider");
            List<Key> ids = List.of(itemIds);
            for (Key itemId : ids)
                this.byId.computeIfAbsent(itemId, ignored -> new CopyOnWriteArrayList<>()).add(provider);
            return Lookups.track(owner, () -> {
                for (Key itemId : ids) {
                    List<ItemProvider<A, C>> list = this.byId.get(itemId);
                    if (list != null) list.remove(provider);
                }
            });
        }

        @Override
        public Registration registerForTag(Plugin owner, ItemProvider<A, C> provider, Key itemTag) {
            return Lookups.addTo(owner, this.byTag, new ItemTagEntry<>(Objects.requireNonNull(itemTag, "itemTag"), Objects.requireNonNull(provider, "provider")));
        }

        @Override
        public <S> Registration registerForSetting(Plugin owner, CustomItemSettingType<S> setting, SettingProvider<S, A, C> provider) {
            return Lookups.addTo(owner, this.settings, new SettingEntry<>(Objects.requireNonNull(setting, "setting"), Objects.requireNonNull(provider, "provider")));
        }

        @Override
        public Registration registerFallback(Plugin owner, ItemProvider<A, C> provider) {
            return Lookups.addTo(owner, this.fallbacks, Objects.requireNonNull(provider, "provider"));
        }

        @Override
        public String toString() {
            return "ItemLookup[" + this.id + "]";
        }
    }

    private record SettingEntry<S, A, C>(CustomItemSettingType<S> type, SettingProvider<S, A, C> provider) {
        @Nullable A find(ItemSettings settings, Item item, @Nullable C context) {
            S value = settings.getCustomData(this.type);
            return value == null ? null : this.provider.find(value, item, context);
        }
    }

    private record ItemTagEntry<A, C>(Key tag, ItemProvider<A, C> provider) {
    }

    private static final class EntityLookupImpl<A, C> implements EntityLookup<A, C> {
        private final Key id;
        private final Class<A> api;
        private final Class<C> context;
        private final Map<Key, List<EntityProvider<A, C>>> byType = new ConcurrentHashMap<>();
        private final List<EntityProvider<A, C>> fallbacks = new CopyOnWriteArrayList<>();

        private EntityLookupImpl(Key id, Class<A> api, Class<C> context) {
            this.id = id;
            this.api = api;
            this.context = context;
        }

        @Override
        public Key id() {
            return this.id;
        }

        @Override
        public @Nullable A find(Entity entity, @Nullable C context) {
            Objects.requireNonNull(entity, "entity");
            List<EntityProvider<A, C>> providers = this.byType.get(entity.type());
            if (providers != null) {
                for (EntityProvider<A, C> provider : providers) {
                    A found = provider.find(entity, context);
                    if (found != null) return found;
                }
            }
            for (EntityProvider<A, C> provider : this.fallbacks) {
                A found = provider.find(entity, context);
                if (found != null) return found;
            }
            return null;
        }

        @Override
        public @Nullable A find(org.bukkit.entity.Entity entity, @Nullable C context) {
            return this.find(BukkitAdaptor.adapt(entity), context);
        }

        @Override
        public Registration registerForTypes(Plugin owner, EntityProvider<A, C> provider, Key... entityTypes) {
            Objects.requireNonNull(provider, "provider");
            List<Key> types = List.of(entityTypes);
            for (Key type : types)
                this.byType.computeIfAbsent(type, ignored -> new CopyOnWriteArrayList<>()).add(provider);
            return Lookups.track(owner, () -> {
                for (Key type : types) {
                    List<EntityProvider<A, C>> list = this.byType.get(type);
                    if (list != null) list.remove(provider);
                }
            });
        }

        @Override
        public Registration registerFallback(Plugin owner, EntityProvider<A, C> provider) {
            return Lookups.addTo(owner, this.fallbacks, Objects.requireNonNull(provider, "provider"));
        }

        @Override
        public String toString() {
            return "EntityLookup[" + this.id + "]";
        }
    }

    private static final class FurnitureLookupImpl<A, C> implements FurnitureLookup<A, C> {
        private final Key id;
        private final Class<A> api;
        private final Class<C> context;
        private final Map<Key, List<FurnitureProvider<A, C>>> byId = new ConcurrentHashMap<>();
        private final List<FurnitureProvider<A, C>> fallbacks = new CopyOnWriteArrayList<>();

        private FurnitureLookupImpl(Key id, Class<A> api, Class<C> context) {
            this.id = id;
            this.api = api;
            this.context = context;
        }

        @Override
        public Key id() {
            return this.id;
        }

        @Override
        public @Nullable A find(Furniture furniture, @Nullable C context) {
            Objects.requireNonNull(furniture, "furniture");
            List<FurnitureProvider<A, C>> providers = this.byId.get(furniture.id());
            if (providers != null) {
                for (FurnitureProvider<A, C> provider : providers) {
                    A found = provider.find(furniture, context);
                    if (found != null) return found;
                }
            }
            for (FurnitureProvider<A, C> provider : this.fallbacks) {
                A found = provider.find(furniture, context);
                if (found != null) return found;
            }
            return null;
        }

        @Override
        public Registration registerForFurniture(Plugin owner, FurnitureProvider<A, C> provider, Key... furnitureIds) {
            Objects.requireNonNull(provider, "provider");
            List<Key> ids = List.of(furnitureIds);
            for (Key furnitureId : ids)
                this.byId.computeIfAbsent(furnitureId, ignored -> new CopyOnWriteArrayList<>()).add(provider);
            return Lookups.track(owner, () -> {
                for (Key furnitureId : ids) {
                    List<FurnitureProvider<A, C>> list = this.byId.get(furnitureId);
                    if (list != null) list.remove(provider);
                }
            });
        }

        @Override
        public Registration registerFallback(Plugin owner, FurnitureProvider<A, C> provider) {
            return Lookups.addTo(owner, this.fallbacks, Objects.requireNonNull(provider, "provider"));
        }

        @Override
        public String toString() {
            return "FurnitureLookup[" + this.id + "]";
        }
    }
}
