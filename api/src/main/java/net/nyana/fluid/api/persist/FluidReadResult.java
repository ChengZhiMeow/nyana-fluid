package net.nyana.fluid.api.persist;

import net.momirealms.craftengine.libraries.nbt.CompoundTag;
import net.momirealms.craftengine.libraries.nbt.Tag;
import net.nyana.fluid.api.fluid.FluidStack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * 一次读取的结果。只有 {@link Status#EMPTY} 和 {@link Status#PRESENT} 可用；另外两种状态下存储会被锁定，
 * {@link #raw()} 原样保留，存盘时原样写回。
 */
public final class FluidReadResult {
    private final Status status;
    private final List<FluidStack> slots;
    private final @Nullable Tag raw;
    private final @Nullable CompoundTag ext;
    private final String message;

    /**
     * 只由编解码器调用。
     */
    @ApiStatus.Internal
    public FluidReadResult(Status status, List<FluidStack> slots, @Nullable Tag raw, @Nullable CompoundTag ext, String message) {
        this.status = Objects.requireNonNull(status, "status");
        this.slots = List.copyOf(slots);
        this.raw = raw == null ? null : raw.deepClone();
        this.ext = ext == null ? null : ext.deepClone();
        this.message = Objects.requireNonNull(message, "message");
    }

    public Status status() {
        return this.status;
    }

    /**
     * 解析出的槽，顺序与数据一致；空槽是 {@link FluidStack#EMPTY}。
     */
    public List<FluidStack> slots() {
        return this.slots;
    }

    public FluidStack first() {
        return this.slots.isEmpty() ? FluidStack.EMPTY : this.slots.get(0);
    }

    /**
     * 原始数据的深拷贝。
     */
    public @Nullable Tag raw() {
        return this.raw == null ? null : this.raw.deepClone();
    }

    /**
     * 扩展区 {@code ext} 的深拷贝。重写数据时应当原样带回。
     */
    public @Nullable CompoundTag ext() {
        return this.ext == null ? null : this.ext.deepClone();
    }

    /**
     * 人类可读的说明，例如为什么是 INVALID。
     */
    public String message() {
        return this.message;
    }

    public boolean usable() {
        return this.status == Status.EMPTY || this.status == Status.PRESENT;
    }

    @Override
    public String toString() {
        return "FluidReadResult[" + this.status + ", " + this.slots + ", " + this.message + "]";
    }

    /**
     * 读取状态。switch 时必须带 default 分支。
     */
    public enum Status {
        /**
         * 没有数据，或者所有槽都为空。
         */
        EMPTY,
        /**
         * 解析成功，流体和组件都已注册。
         */
        PRESENT,
        /**
         * 结构合法，但版本更新、缺少迁移、流体未注册或组件没有 codec。锁定。
         */
        UNKNOWN,
        /**
         * 结构不合法。锁定。
         */
        INVALID
    }
}
