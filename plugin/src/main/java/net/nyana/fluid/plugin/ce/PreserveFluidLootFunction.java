package net.nyana.fluid.plugin.ce;

import net.momirealms.craftengine.core.block.entity.BlockEntity;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.loot.LootContext;
import net.momirealms.craftengine.core.loot.function.LootFunction;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.CEWorld;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.libraries.nbt.CompoundTag;
import net.momirealms.craftengine.libraries.nbt.Tag;
import net.nyana.fluid.api.block.FluidTankController;
import net.nyana.fluid.api.internal.DropCache;
import net.nyana.fluid.api.internal.Lookups;
import net.nyana.fluid.api.persist.FluidDataSlot;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

final class PreserveFluidLootFunction implements LootFunction {
    private static @Nullable Tag live(@Nullable CEWorld world, BlockPos pos) {
        if (world == null) return null;
        BlockEntity blockEntity = world.getBlockEntityAtIfLoaded(pos);
        if (blockEntity == null || blockEntity.controller == null) return null;
        List<FluidTankController> tanks = Lookups.controllers(blockEntity.controller, FluidTankController.class);
        if (tanks.isEmpty() || !tanks.get(0).spec().keepOnBreak()) return null;
        CompoundTag data = new CompoundTag();
        tanks.get(0).saveCustomData(data);
        Tag fluid = data.get(tanks.get(0).spec().dataKey());
        return PreserveFluidLootFunction.isEmptyRecord(fluid) ? null : fluid;
    }

    private static boolean isEmptyRecord(@Nullable Tag tag) {
        return tag == null || FluidDataSlot.read(PreserveFluidLootFunction.wrap(tag)).status() == net.nyana.fluid.api.persist.FluidReadResult.Status.EMPTY;
    }

    private static CompoundTag wrap(Tag tag) {
        CompoundTag wrapper = new CompoundTag();
        wrapper.put(net.nyana.fluid.api.persist.FluidDataFormat.ROOT_KEY, tag);
        return wrapper;
    }

    @Override
    public Item apply(Item item, LootContext context) {
        if (item == null || item.isEmpty()) return item;
        Optional<WorldPosition> position = context.getOptionalParameter(DirectContextParameters.POSITION);
        if (position.isEmpty()) return item;
        WorldPosition where = position.get();
        int x = (int) Math.floor(where.x());
        int y = (int) Math.floor(where.y());
        int z = (int) Math.floor(where.z());
        Tag data = PreserveFluidLootFunction.live(where.world().storageWorld(), new BlockPos(x, y, z));
        if (data == null) data = DropCache.take(where.world().uuid(), x, y, z);
        if (data == null) return item;
        Item copy = item.copy();
        FluidDataSlot.writeRaw(copy, data);
        return copy;
    }
}
