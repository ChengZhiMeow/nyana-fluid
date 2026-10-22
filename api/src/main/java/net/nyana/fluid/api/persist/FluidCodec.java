package net.nyana.fluid.api.persist;

import net.momirealms.craftengine.libraries.nbt.CompoundTag;
import net.momirealms.craftengine.libraries.nbt.Tag;
import net.nyana.fluid.api.Registration;
import net.nyana.fluid.api.fluid.FluidStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.UnaryOperator;

/**
 * 流体数据的 NBT 编解码器。编码出来的是根键下面那一层（{@code {v, slots, ext}}），由
 * {@link FluidDataSlot} 负责放到物品或方块数据的正确位置。
 */
@ApiStatus.NonExtendable
public interface FluidCodec {
    /**
     * 编码若干槽。
     *
     * @throws IllegalArgumentException 超出 {@link FluidDataFormat} 的上限
     */
    CompoundTag encode(List<FluidStack> slots);

    /**
     * 编码若干槽，并带回扩展区。
     *
     * @throws IllegalArgumentException 超出 {@link FluidDataFormat} 的上限
     */
    CompoundTag encode(List<FluidStack> slots, @Nullable CompoundTag ext);

    CompoundTag encode(FluidStack stack);

    /**
     * 解码。永远不抛异常，结果见 {@link FluidReadResult.Status}。
     */
    FluidReadResult decode(@Nullable Tag tag);

    /**
     * 注册一步迁移：输入版本 {@code fromVersion} 的完整数据，输出版本更新的完整数据（必须修改 {@code v}）。
     * 迁移只在读取时执行；迁移函数抛出异常时，数据读成 {@code INVALID}。
     *
     * @throws IllegalStateException 这个版本已经有迁移
     */
    Registration registerMigration(Plugin owner, int fromVersion, UnaryOperator<CompoundTag> migration);
}
