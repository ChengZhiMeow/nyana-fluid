package net.nyana.fluid.api.storage.base;

import net.nyana.fluid.api.fluid.FluidVariant;
import net.nyana.fluid.api.internal.Preconditions;
import net.nyana.fluid.api.storage.FluidSlotView;
import net.nyana.fluid.api.storage.FluidStorage;
import net.nyana.fluid.api.transaction.TransactionContext;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 把多个存储组合成一个：插入和抽取按顺序依次尝试各个部分, 遍历时依次列出各部分的槽,.
 */
public class CombinedFluidStorage implements FluidStorage {
    private final List<FluidStorage> parts;

    public CombinedFluidStorage(List<? extends FluidStorage> parts) {
        this.parts = List.copyOf(parts);
    }

    public List<FluidStorage> parts() {
        return this.parts;
    }

    @Override
    public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        Preconditions.checkResource(resource, maxAmount);
        long inserted = 0;
        for (FluidStorage part : this.parts) {
            if (inserted >= maxAmount) break;
            long remaining = maxAmount - inserted;
            inserted += Preconditions.checkResult(part.insert(resource, remaining, transaction), remaining);
        }
        return inserted;
    }

    @Override
    public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        Preconditions.checkResource(resource, maxAmount);
        long extracted = 0;
        for (FluidStorage part : this.parts) {
            if (extracted >= maxAmount) break;
            long remaining = maxAmount - extracted;
            extracted += Preconditions.checkResult(part.extract(resource, remaining, transaction), remaining);
        }
        return extracted;
    }

    @Override
    public Iterator<FluidSlotView> iterator() {
        List<FluidSlotView> views = new ArrayList<>();
        for (FluidStorage part : this.parts) for (FluidSlotView view : part) views.add(view);
        return views.iterator();
    }

    @Override
    public boolean supportsInsertion() {
        for (FluidStorage part : this.parts) if (part.supportsInsertion()) return true;
        return false;
    }

    @Override
    public boolean supportsExtraction() {
        for (FluidStorage part : this.parts) if (part.supportsExtraction()) return true;
        return false;
    }

    @Override
    public long version() {
        long version = 0;
        for (FluidStorage part : this.parts) version += part.version();
        return version;
    }

    @Override
    public boolean isLocked() {
        for (FluidStorage part : this.parts) if (part.isLocked()) return true;
        return false;
    }
}
