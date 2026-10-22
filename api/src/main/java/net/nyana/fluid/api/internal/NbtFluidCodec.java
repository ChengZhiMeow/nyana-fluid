package net.nyana.fluid.api.internal;

import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.libraries.nbt.*;
import net.nyana.fluid.api.Registration;
import net.nyana.fluid.api.fluid.FluidRegistry;
import net.nyana.fluid.api.fluid.FluidStack;
import net.nyana.fluid.api.fluid.FluidVariant;
import net.nyana.fluid.api.fluid.component.FluidComponentRegistry;
import net.nyana.fluid.api.fluid.component.FluidComponentType;
import net.nyana.fluid.api.fluid.component.FluidComponents;
import net.nyana.fluid.api.persist.FluidCodec;
import net.nyana.fluid.api.persist.FluidDataException;
import net.nyana.fluid.api.persist.FluidDataFormat;
import net.nyana.fluid.api.persist.FluidReadResult;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

/**
 * DATA-FORMAT v1. Strict when writing, lenient when reading, and decode never throws.
 */
@ApiStatus.Internal
public final class NbtFluidCodec implements FluidCodec {
    private static final Key BLANK = Key.of("minecraft", "empty");

    private static CompoundTag encodeSlot(FluidStack stack) {
        CompoundTag slot = new CompoundTag();
        if (stack.isEmpty()) return slot;
        FluidVariant variant = stack.variant();
        slot.putString("id", NbtFluidCodec.checkKey(variant.fluidKey()));
        slot.putLong("amount", stack.amount());
        FluidComponents components = variant.components();
        if (!components.isEmpty()) {
            if (components.size() > FluidDataFormat.MAX_COMPONENTS)
                throw new IllegalArgumentException("Too many components: " + components.size());
            CompoundTag encoded = new CompoundTag();
            for (Key key : components.keys())
                encoded.put(NbtFluidCodec.checkKey(key), components.raw(key).orElseThrow());
            slot.put("components", encoded);
        }
        return slot;
    }

    private static String checkKey(Key key) {
        String text = key.toString();
        if (text.length() > FluidDataFormat.MAX_KEY_LENGTH)
            throw new IllegalArgumentException("Key is too long: " + text);
        return text;
    }

    private static FluidReadResult unknown(Tag raw, String message) {
        return new FluidReadResult(FluidReadResult.Status.UNKNOWN, List.of(), raw, null, message);
    }

    private static int version(CompoundTag tag) throws FluidDataException {
        if (!(tag.get("v") instanceof NumericTag number)) throw new FluidDataException("Missing data version");
        return number.getAsInt();
    }

    private static Key readKey(@Nullable Tag tag, String where) throws FluidDataException {
        if (!(tag instanceof StringTag string)) throw new FluidDataException(where + " is not a string");
        return NbtFluidCodec.parseKey(string.value(), where);
    }

    private static Key parseKey(String text, String where) throws FluidDataException {
        if (text.isEmpty() || text.length() > FluidDataFormat.MAX_KEY_LENGTH || text.indexOf(':') <= 0) {
            throw new FluidDataException(where + " is not a namespaced key: " + text);
        }
        try {
            return Key.of(text);
        } catch (RuntimeException exception) {
            throw new FluidDataException(where + " is not a namespaced key: " + text, exception);
        }
    }

    private final FluidRegistry fluids;
    private final FluidComponentRegistry components;
    private final Registrations registrations;
    private final Map<Integer, UnaryOperator<CompoundTag>> migrations = new ConcurrentHashMap<>();

    public NbtFluidCodec(FluidRegistry fluids, FluidComponentRegistry components, Registrations registrations) {
        this.fluids = Objects.requireNonNull(fluids, "fluids");
        this.components = Objects.requireNonNull(components, "components");
        this.registrations = Objects.requireNonNull(registrations, "registrations");
    }

    @Override
    public CompoundTag encode(List<FluidStack> slots) {
        return this.encode(slots, null);
    }

    @Override
    public CompoundTag encode(FluidStack stack) {
        return this.encode(List.of(stack), null);
    }

    @Override
    public CompoundTag encode(List<FluidStack> slots, @Nullable CompoundTag ext) {
        Objects.requireNonNull(slots, "slots");
        if (slots.size() > FluidDataFormat.MAX_SLOTS)
            throw new IllegalArgumentException("Too many slots: " + slots.size());
        CompoundTag root = new CompoundTag();
        root.putInt("v", FluidDataFormat.CURRENT_VERSION);
        ListTag list = new ListTag();
        for (FluidStack stack : slots)
            list.add(list.size(), NbtFluidCodec.encodeSlot(Objects.requireNonNull(stack, "slot")));
        root.put("slots", list);
        if (ext != null && !ext.isEmpty()) root.put("ext", ext.deepClone());
        return root;
    }

    @Override
    public FluidReadResult decode(@Nullable Tag tag) {
        if (tag == null) return new FluidReadResult(FluidReadResult.Status.EMPTY, List.of(), null, null, "No data");
        try {
            return this.decodeChecked(tag);
        } catch (FluidDataException exception) {
            return new FluidReadResult(FluidReadResult.Status.INVALID, List.of(), tag, null, exception.getMessage());
        } catch (RuntimeException exception) {
            return new FluidReadResult(FluidReadResult.Status.INVALID, List.of(), tag, null,
                    exception.getClass().getSimpleName() + ": " + exception.getMessage());
        }
    }

    private FluidReadResult decodeChecked(Tag tag) throws FluidDataException {
        if (!(tag instanceof CompoundTag root)) throw new FluidDataException("Root is not a compound");
        CompoundTag current = root;
        int version = NbtFluidCodec.version(current);
        for (int steps = 0; version < FluidDataFormat.CURRENT_VERSION; steps++) {
            if (steps >= FluidDataFormat.MAX_MIGRATION_STEPS) throw new FluidDataException("Too many migration steps");
            UnaryOperator<CompoundTag> migration = this.migrations.get(version);
            if (migration == null) return NbtFluidCodec.unknown(tag, "No migration from data version " + version);
            CompoundTag migrated = migration.apply(current.deepClone());
            if (migrated == null) throw new FluidDataException("Migration from version " + version + " returned null");
            int next = NbtFluidCodec.version(migrated);
            if (next <= version)
                throw new FluidDataException("Migration from version " + version + " did not advance the version");
            current = migrated;
            version = next;
        }
        if (version > FluidDataFormat.CURRENT_VERSION)
            return NbtFluidCodec.unknown(tag, "Written by a newer data version " + version);

        if (!(current.get("slots") instanceof ListTag list)) throw new FluidDataException("Missing slots list");
        if (list.size() > FluidDataFormat.MAX_SLOTS) throw new FluidDataException("Too many slots: " + list.size());
        Tag extTag = current.get("ext");
        if (extTag != null && !(extTag instanceof CompoundTag)) throw new FluidDataException("ext is not a compound");
        CompoundTag ext = (CompoundTag) extTag;

        boolean unknown = false;
        String unknownReason = "";
        List<FluidStack> stacks = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            if (!(list.get(i) instanceof CompoundTag slot))
                throw new FluidDataException("Slot " + i + " is not a compound");
            if (slot.isEmpty()) {
                stacks.add(FluidStack.EMPTY);
                continue;
            }
            Key id = NbtFluidCodec.readKey(slot.get("id"), "slot " + i + " id");
            if (id.equals(NbtFluidCodec.BLANK))
                throw new FluidDataException("Slot " + i + " uses the empty fluid with an amount");
            if (!(slot.get("amount") instanceof NumericTag amountTag))
                throw new FluidDataException("Slot " + i + " has no numeric amount");
            long amount = amountTag.getAsLong();
            if (amount <= 0) throw new FluidDataException("Slot " + i + " has a non-positive amount");
            FluidComponents.Builder builder = FluidComponents.builder();
            Tag componentsTag = slot.get("components");
            if (componentsTag != null) {
                if (!(componentsTag instanceof CompoundTag componentTags))
                    throw new FluidDataException("Slot " + i + " components is not a compound");
                if (componentTags.size() > FluidDataFormat.MAX_COMPONENTS)
                    throw new FluidDataException("Slot " + i + " has too many components");
                for (String name : componentTags.keySet()) {
                    Key componentKey = NbtFluidCodec.parseKey(name, "slot " + i + " component");
                    Tag value = componentTags.get(name);
                    Optional<FluidComponentType<?>> type = this.components.get(componentKey);
                    if (type.isEmpty()) {
                        unknown = true;
                        unknownReason = "Unknown component " + componentKey;
                    } else {
                        type.get().codec().decode(value.deepClone());
                    }
                    builder.putRaw(componentKey, value);
                }
            }
            if (!this.fluids.contains(id)) {
                unknown = true;
                unknownReason = "Unknown fluid " + id;
            }
            stacks.add(FluidStack.of(FluidVariant.of(id, builder.build()), amount));
        }
        if (unknown) return new FluidReadResult(FluidReadResult.Status.UNKNOWN, stacks, tag, ext, unknownReason);
        boolean empty = stacks.stream().allMatch(FluidStack::isEmpty);
        return new FluidReadResult(empty ? FluidReadResult.Status.EMPTY : FluidReadResult.Status.PRESENT, stacks, tag, ext, "Decoded");
    }

    @Override
    public Registration registerMigration(Plugin owner, int fromVersion, UnaryOperator<CompoundTag> migration) {
        Objects.requireNonNull(migration, "migration");
        if (fromVersion < 0 || fromVersion >= FluidDataFormat.CURRENT_VERSION) {
            throw new IllegalArgumentException("A migration must start from a version older than " + FluidDataFormat.CURRENT_VERSION);
        }
        if (this.migrations.putIfAbsent(fromVersion, migration) != null) {
            throw new IllegalStateException("A migration from data version " + fromVersion + " is already registered");
        }
        return this.registrations.register(owner, () -> this.migrations.remove(fromVersion, migration));
    }
}
