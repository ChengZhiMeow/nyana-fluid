package net.nyana.fluid.api.item;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.libraries.nbt.CompoundTag;
import net.nyana.fluid.api.fluid.FluidStack;
import net.nyana.fluid.api.fluid.FluidVariant;
import net.nyana.fluid.api.internal.Preconditions;
import net.nyana.fluid.api.persist.FluidDataSlot;
import net.nyana.fluid.api.persist.FluidReadResult;
import net.nyana.fluid.api.storage.FluidSlot;
import net.nyana.fluid.api.storage.FluidSlotView;
import net.nyana.fluid.api.storage.SlottedFluidStorage;
import net.nyana.fluid.api.transaction.TransactionContext;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * 数据型容器：流体写在物品 {@code custom_data} 的 {@code "nyana-fluid"} 下,.每次修改都会生成一个新物品,
 * 通过 {@link ItemAccess#exchange} 换掉原物品, 所以修改和其他存储的操作在同一个事务里原子完成,.
 *
 * <p>物品上的数据读成 {@code UNKNOWN} / {@code INVALID} 时存储被锁定：拒绝读写, 原数据不动,.
 * 只接受已注册的流体；有 {@link ContainerSpec#filter()} 时还要满足过滤,.
 */
public class ItemFluidStorage implements SlottedFluidStorage {
    private final ItemAccess access;
    private final ContainerSpec spec;

    public ItemFluidStorage(ItemAccess access, ContainerSpec spec) {
        this.access = Objects.requireNonNull(access, "access");
        this.spec = Objects.requireNonNull(spec, "spec");
    }

    public ContainerSpec spec() {
        return this.spec;
    }

    public ItemAccess access() {
        return this.access;
    }

    private FluidReadResult read() {
        return FluidDataSlot.read(this.access.item());
    }

    private List<FluidStack> contents(FluidReadResult result) {
        List<FluidStack> slots = new ArrayList<>(this.spec.slots());
        for (int i = 0; i < this.spec.slots(); i++)
            slots.add(i < result.slots().size() ? result.slots().get(i) : FluidStack.EMPTY);
        return slots;
    }

    /**
     * 是否接受某个变体,.默认要求已注册、满足规格里的过滤,.
     */
    protected boolean canInsert(FluidVariant resource) {
        return resource.fluid().isPresent() && this.spec.filter().map(filter -> filter.test(resource)).orElse(true);
    }

    @Override
    public boolean isLocked() {
        return !this.read().usable();
    }

    @Override
    public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        return this.insertInto(-1, resource, maxAmount, transaction);
    }

    @Override
    public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        return this.extractFrom(-1, resource, maxAmount, transaction);
    }

    private long insertInto(int only, FluidVariant resource, long maxAmount, TransactionContext transaction) {
        Preconditions.checkResource(resource, maxAmount);
        if (maxAmount == 0 || this.access.amount() == 0 || !this.canInsert(resource)) return 0;
        FluidReadResult result = this.read();
        if (!result.usable()) return 0;
        List<FluidStack> slots = this.contents(result);
        long inserted = 0;
        for (int pass = 0; pass < 2 && inserted < maxAmount; pass++) {
            for (int i = 0; i < slots.size() && inserted < maxAmount; i++) {
                if (only >= 0 && i != only) continue;
                FluidStack slot = slots.get(i);
                boolean same = !slot.isEmpty() && slot.variant().equals(resource);
                if (pass == 0 ? !same : !slot.isEmpty()) continue;
                long add = Math.min(maxAmount - inserted, this.spec.capacity() - slot.amount());
                if (add <= 0) continue;
                slots.set(i, slot.isEmpty() ? FluidStack.of(resource, add) : slot.grow(add));
                inserted += add;
            }
        }
        return inserted > 0 && this.replace(slots, result.ext(), transaction) ? inserted : 0;
    }

    private long extractFrom(int only, FluidVariant resource, long maxAmount, TransactionContext transaction) {
        Preconditions.checkResource(resource, maxAmount);
        if (maxAmount == 0 || this.access.amount() == 0) return 0;
        FluidReadResult result = this.read();
        if (!result.usable()) return 0;
        List<FluidStack> slots = this.contents(result);
        long extracted = 0;
        for (int i = 0; i < slots.size() && extracted < maxAmount; i++) {
            if (only >= 0 && i != only) continue;
            FluidStack slot = slots.get(i);
            if (slot.isEmpty() || !slot.variant().equals(resource)) continue;
            long take = Math.min(maxAmount - extracted, slot.amount());
            slots.set(i, slot.shrink(take));
            extracted += take;
        }
        return extracted > 0 && this.replace(slots, result.ext(), transaction) ? extracted : 0;
    }

    private boolean replace(List<FluidStack> slots, CompoundTag ext, TransactionContext transaction) {
        Item replacement = this.access.item().copyWithCount(1);
        FluidDataSlot.write(replacement, slots, ext);
        return this.access.exchange(replacement, 1, transaction) == 1;
    }

    @Override
    public Iterator<FluidSlotView> iterator() {
        List<FluidSlotView> views = new ArrayList<>(this.spec.slots());
        for (int i = 0; i < this.spec.slots(); i++) views.add(new Slot(i));
        return views.iterator();
    }

    @Override
    public int slotCount() {
        return this.spec.slots();
    }

    @Override
    public FluidSlot slot(int index) {
        Objects.checkIndex(index, this.spec.slots());
        return new Slot(index);
    }

    private final class Slot implements FluidSlot {
        private final int index;

        private Slot(int index) {
            this.index = index;
        }

        private FluidStack stack() {
            FluidReadResult result = ItemFluidStorage.this.read();
            return result.usable() ? ItemFluidStorage.this.contents(result).get(this.index) : FluidStack.EMPTY;
        }

        @Override
        public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            return ItemFluidStorage.this.insertInto(this.index, resource, maxAmount, transaction);
        }

        @Override
        public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            return ItemFluidStorage.this.extractFrom(this.index, resource, maxAmount, transaction);
        }

        @Override
        public FluidVariant variant() {
            return this.stack().variant();
        }

        @Override
        public long amount() {
            return this.stack().amount();
        }

        @Override
        public long capacity() {
            return ItemFluidStorage.this.spec.capacity();
        }

        @Override
        public boolean isLocked() {
            return ItemFluidStorage.this.isLocked();
        }
    }
}
