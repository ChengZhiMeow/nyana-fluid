package net.nyana.fluid.api.persist;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.libraries.nbt.CompoundTag;
import net.momirealms.craftengine.libraries.nbt.Tag;
import net.nyana.fluid.api.NyanaFluid;
import net.nyana.fluid.api.fluid.FluidStack;
import net.nyana.fluid.api.platform.ItemDataBridge;
import net.nyana.fluid.api.platform.PlatformBridge;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * 读写物品 / 方块上的流体数据；已有数据读不懂时拒绝写入。
 */
public final class FluidDataSlot {
    /**
     * 读取物品上的流体数据。
     */
    public static FluidReadResult read(Item item) {
        Objects.requireNonNull(item, "item");
        return NyanaFluid.codec().decode(FluidDataSlot.readItemTag(item));
    }

    /**
     * 写入物品上的流体数据，覆盖原有内容（扩展区原样保留）。
     */
    public static boolean write(Item item, List<FluidStack> slots) {
        FluidReadResult existing = FluidDataSlot.read(item);
        if (!existing.usable()) return false;
        return FluidDataSlot.write(item, slots, existing.ext());
    }

    /**
     * 写入物品上的流体数据，并指定扩展区。不检查已有数据，调用前请自行确认已有数据可用。
     */
    public static boolean write(Item item, List<FluidStack> slots, @Nullable CompoundTag ext) {
        Objects.requireNonNull(item, "item");
        boolean empty = slots.stream().allMatch(FluidStack::isEmpty) && (ext == null || ext.isEmpty());
        FluidDataSlot.writeItemTag(item, empty ? null : NyanaFluid.codec().encode(slots, ext));
        return true;
    }

    /**
     * 把锁定时保留的原始数据原样写回物品。
     */
    public static void writeRaw(Item item, @Nullable Tag raw) {
        FluidDataSlot.writeItemTag(Objects.requireNonNull(item, "item"), raw == null ? null : raw.deepClone());
    }

    /**
     * 读取方块数据里默认 {@code data_key} 下的流体数据。
     */
    public static FluidReadResult read(CompoundTag data) {
        return FluidDataSlot.read(data, FluidDataFormat.ROOT_KEY);
    }

    /**
     * 读取方块数据里指定 key 下的流体数据。
     */
    public static FluidReadResult read(CompoundTag data, String key) {
        Objects.requireNonNull(data, "data");
        return NyanaFluid.codec().decode(data.get(Objects.requireNonNull(key, "key")));
    }

    /**
     * 写入方块数据里默认 {@code data_key} 下的流体数据（带写入保护）。
     */
    public static boolean write(CompoundTag data, List<FluidStack> slots) {
        FluidReadResult existing = FluidDataSlot.read(data);
        if (!existing.usable()) return false;
        FluidDataSlot.write(data, FluidDataFormat.ROOT_KEY, slots, existing.ext());
        return true;
    }

    /**
     * 写入方块数据里指定 key 下的流体数据，不检查已有数据。
     */
    public static void write(CompoundTag data, String key, List<FluidStack> slots, @Nullable CompoundTag ext) {
        Objects.requireNonNull(data, "data");
        data.put(Objects.requireNonNull(key, "key"), NyanaFluid.codec().encode(slots, ext));
    }

    /**
     * 把锁定时保留的原始数据原样写回方块数据。
     */
    public static void writeRaw(CompoundTag data, String key, @Nullable Tag raw) {
        Objects.requireNonNull(data, "data");
        if (raw == null) data.remove(key);
        else data.put(key, raw.deepClone());
    }

    private static @Nullable Tag readItemTag(Item item) {
        PlatformBridge bridge = NyanaFluid.service().bridge();
        if (bridge != null) return bridge.itemData().read(item, FluidDataFormat.ROOT_KEY);
        return ItemDataBridge.direct().read(item, FluidDataFormat.ROOT_KEY);
    }

    private static void writeItemTag(Item item, @Nullable Tag tag) {
        PlatformBridge bridge = NyanaFluid.service().bridge();
        ItemDataBridge data = bridge != null ? bridge.itemData() : ItemDataBridge.direct();
        data.write(item, FluidDataFormat.ROOT_KEY, tag);
    }

    private FluidDataSlot() {
    }
}
