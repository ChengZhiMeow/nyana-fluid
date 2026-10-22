package net.nyana.fluid.api.block;

import net.momirealms.craftengine.core.block.entity.BlockEntity;
import net.momirealms.craftengine.core.block.entity.BlockEntityController;
import net.momirealms.craftengine.core.block.entity.render.element.BlockEntityElement;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.libraries.nbt.CompoundTag;
import net.momirealms.craftengine.libraries.nbt.Tag;
import net.nyana.fluid.api.NyanaFluid;
import net.nyana.fluid.api.fluid.FluidStack;
import net.nyana.fluid.api.fluid.FluidVariant;
import net.nyana.fluid.api.internal.DropCache;
import net.nyana.fluid.api.internal.Regions;
import net.nyana.fluid.api.persist.FluidDataSlot;
import net.nyana.fluid.api.persist.FluidReadResult;
import net.nyana.fluid.api.render.DefaultFluidTankDisplay;
import net.nyana.fluid.api.render.FluidTankDisplay;
import net.nyana.fluid.api.storage.FluidSlotView;
import net.nyana.fluid.api.storage.FluidStorage;
import net.nyana.fluid.api.storage.SlottedFluidStorage;
import net.nyana.fluid.api.storage.base.FluidStorages;
import net.nyana.fluid.api.storage.base.MultiFluidTank;
import net.nyana.fluid.api.storage.base.SidedFluidStorage;
import net.nyana.fluid.api.transaction.TransactionContext;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * 储罐 controller：存读档、锁定（数据读不懂时原样保留）、合并刷新、选中的流体，以及配置了 {@code renderer} 时的流体展示。
 * 不 tick；做机器时覆写 {@code createBlockEntityTicker} 和 {@link #onContentsChanged()}，换展示覆写 {@link #createDisplay()}。
 */
public class FluidTankController extends BlockEntityController implements FluidBlockEntity {
    private static final String SELECTED = "selected";
    private final TankSpec spec;
    private final TankStorage tanks;
    private final SidedFluidStorage sided;
    private FluidReadResult.Status dataStatus = FluidReadResult.Status.EMPTY;
    private @Nullable Tag lockedRaw;
    private @Nullable CompoundTag ext;
    private long flushedVersion;
    private int selected = -1;
    private @Nullable FluidTankDisplay display;
    private boolean displayCreated;

    public FluidTankController(BlockEntity blockEntity, TankSpec spec) {
        super(blockEntity);
        this.spec = Objects.requireNonNull(spec, "spec");
        this.tanks = new TankStorage();
        this.sided = FluidStorages.sided(this.tanks, spec.sides());
    }

    public TankSpec spec() {
        return this.spec;
    }

    /**
     * 内部完整访问，不受面配置限制（仍受锁定和过滤限制）。
     */
    public SlottedFluidStorage tanks() {
        return this.tanks;
    }

    @Override
    public @Nullable FluidStorage fluidStorage(@Nullable Direction side) {
        return this.sided.side(side);
    }

    /**
     * 总容量（共用容量时就是那一份）。
     */
    public long totalCapacity() {
        return this.tanks.totalCapacity();
    }

    /**
     * 选中的槽，-1 表示没有选中。选中的流体显示在最下面，取出时优先取它。
     */
    public int selected() {
        return this.selected;
    }

    /**
     * 选中一个槽；-1 取消选中。
     */
    public void select(int slot) {
        if (slot < -1 || slot >= this.spec.slots()) throw new IllegalArgumentException("No slot " + slot);
        if (this.selected == slot) return;
        this.selected = slot;
        if (this.ext == null) this.ext = new CompoundTag();
        if (slot < 0) this.ext.remove(FluidTankController.SELECTED);
        else this.ext.putInt(FluidTankController.SELECTED, slot);
        BlockEntity entity = this.blockEntity();
        if (entity != null && entity.world != null) entity.world.blockEntityChanged(entity.pos);
        FluidTankDisplay shown = this.display();
        if (shown != null) shown.refresh(true);
    }

    /**
     * 有流体的槽，从下往上：选中的在最下面，其余按槽的顺序。
     */
    public List<Integer> displayOrder() {
        List<Integer> order = new ArrayList<>(this.spec.slots());
        if (this.selected >= 0 && !this.tanks.slot(this.selected).isBlank()) order.add(this.selected);
        for (int i = 0; i < this.spec.slots(); i++)
            if (i != this.selected && !this.tanks.slot(i).isBlank()) order.add(i);
        return order;
    }

    /**
     * 储罐里的流体展示；没有配置 {@code renderer} 时为 {@code null}。
     */
    public final @Nullable FluidTankDisplay display() {
        if (!this.displayCreated) {
            this.displayCreated = true;
            this.display = this.createDisplay();
        }
        return this.display;
    }

    /**
     * 创建流体展示。默认按 {@link TankSpec#renderer()} 用 {@link DefaultFluidTankDisplay}；覆写可以换成自己的展示。
     */
    protected @Nullable FluidTankDisplay createDisplay() {
        return this.spec.renderer().<FluidTankDisplay>map(renderer -> new DefaultFluidTankDisplay(this, renderer)).orElse(null);
    }

    @Override
    public boolean hasElement() {
        return this.display() != null || super.hasElement();
    }

    @Override
    public void gatherElements(Consumer<BlockEntityElement> consumer) {
        super.gatherElements(consumer);
        FluidTankDisplay shown = this.display();
        if (shown != null) consumer.accept(shown);
    }

    /**
     * 最近一次读档的状态。{@code UNKNOWN} / {@code INVALID} 时储罐被锁定。
     */
    public FluidReadResult.Status dataStatus() {
        return this.dataStatus;
    }

    public boolean isLocked() {
        return this.lockedRaw != null;
    }

    /**
     * 内容在一次合并刷新里发生变化后调用（每 tick 最多一次），在方块所在的区域线程上。默认什么也不做。
     * 子类在这里唤醒自己的 ticker、改外观、通知相邻方块等。
     */
    protected void onContentsChanged() {
    }

    @Override
    public void saveCustomData(CompoundTag data) {
        if (this.lockedRaw != null) {
            FluidDataSlot.writeRaw(data, this.spec.dataKey(), this.lockedRaw);
        } else {
            FluidDataSlot.write(data, this.spec.dataKey(), this.contents(), this.ext);
        }
    }

    @Override
    public void loadCustomData(CompoundTag data) {
        this.load(FluidDataSlot.read(data, this.spec.dataKey()));
    }

    @Override
    public void loadCustomDataFromItem(Item item) {
        this.load(FluidDataSlot.read(item));
    }

    private void load(FluidReadResult result) {
        this.dataStatus = result.status();
        this.ext = result.ext();
        int stored = this.ext == null ? -1 : this.ext.getInt(FluidTankController.SELECTED, -1);
        this.selected = stored >= -1 && stored < this.spec.slots() ? stored : -1;
        boolean tooManySlots = false;
        for (int i = this.spec.slots(); i < result.slots().size(); i++)
            if (!result.slots().get(i).isEmpty()) tooManySlots = true;
        if (!result.usable() || tooManySlots) {
            // Unknown, invalid, or more filled slots than the configuration now has: keep the record untouched.
            this.lockedRaw = result.raw();
            if (this.lockedRaw == null) this.lockedRaw = NyanaFluid.codec().encode(result.slots(), result.ext());
            for (int i = 0; i < this.spec.slots(); i++) this.tanks.tank(i).setStack(FluidStack.EMPTY);
            return;
        }
        this.lockedRaw = null;
        for (int i = 0; i < this.spec.slots(); i++)
            this.tanks.tank(i).setStack(i < result.slots().size() ? result.slots().get(i) : FluidStack.EMPTY);
        this.flushedVersion = this.tanks.version();
        FluidTankDisplay shown = this.display();
        if (shown != null) shown.refresh(false);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        FluidTankDisplay shown = this.display();
        if (shown != null) shown.refresh(false);
    }

    @Override
    public void onRemove() {
        FluidTankDisplay shown = this.display();
        if (shown != null) shown.deactivate();
        BlockEntity entity = this.blockEntity();
        if (this.spec.keepOnBreak() && entity != null && entity.world != null) {
            Tag data = this.lockedRaw != null ? this.lockedRaw : (this.isEmpty() ? null : NyanaFluid.codec().encode(this.contents(), this.ext));
            if (data != null) DropCache.put(entity.world.uuid(), entity.pos, data);
        }
        super.onRemove();
    }

    /**
     * 合并刷新：只由 NyanaFluid 在方块所在的区域线程上调用。标脏存盘、调用 {@link #onContentsChanged()}。
     */
    @ApiStatus.Internal
    public final boolean flush() {
        BlockEntity entity = this.blockEntity();
        if (entity == null || !entity.isValid() || entity.world == null) return false;
        long version = this.tanks.version();
        if (version == this.flushedVersion) return false;
        this.flushedVersion = version;
        entity.world.blockEntityChanged(entity.pos);
        FluidTankDisplay shown = this.display();
        if (shown != null) shown.refresh(true);
        this.onContentsChanged();
        return true;
    }

    private List<FluidStack> contents() {
        List<FluidStack> contents = new ArrayList<>(this.spec.slots());
        for (int i = 0; i < this.spec.slots(); i++) contents.add(this.tanks.tank(i).stack());
        return contents;
    }

    private boolean isEmpty() {
        for (FluidStack stack : this.contents()) if (!stack.isEmpty()) return false;
        return true;
    }

    private void checkRegion() {
        BlockEntity entity = this.blockEntity();
        if (entity == null || entity.world == null) return;
        if (entity.world.world().platformWorld() instanceof org.bukkit.World bukkit)
            Regions.checkChunk(bukkit, entity.pos.x() >> 4, entity.pos.z() >> 4);
    }

    private final class TankStorage extends MultiFluidTank {
        private TankStorage() {
            super(FluidTankController.this.spec.slots(), FluidTankController.this.spec.capacity(), FluidTankController.this.spec.sharedCapacity());
        }

        @Override
        public Iterator<FluidSlotView> iterator() {
            List<FluidSlotView> views = new ArrayList<>(super.slotCount());
            int first = FluidTankController.this.selected;
            if (first >= 0) views.add(super.slot(first));
            for (int i = 0; i < super.slotCount(); i++) if (i != first) views.add(super.slot(i));
            return views.iterator();
        }

        @Override
        protected boolean canInsert(int slot, FluidVariant resource) {
            return super.canInsert(slot, resource) && FluidTankController.this.spec.filter().map(filter -> filter.test(resource)).orElse(true);
        }

        @Override
        public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            return this.isLocked() ? 0 : super.insert(resource, maxAmount, transaction);
        }

        @Override
        public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            return this.isLocked() ? 0 : super.extract(resource, maxAmount, transaction);
        }

        @Override
        public boolean isLocked() {
            return FluidTankController.this.isLocked();
        }

        @Override
        protected void checkAccess() {
            FluidTankController.this.checkRegion();
        }

        @Override
        protected void onChanged() {
            NyanaFluid.service().markChanged(FluidTankController.this);
        }
    }
}
