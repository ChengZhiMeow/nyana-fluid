package net.nyana.fluid.plugin.builtin;

import net.momirealms.craftengine.core.world.ExistingBlock;
import net.nyana.fluid.api.fluid.FluidAmount;
import net.nyana.fluid.api.fluid.FluidVariant;
import net.nyana.fluid.api.internal.Preconditions;
import net.nyana.fluid.api.internal.Regions;
import net.nyana.fluid.api.storage.FluidSlotView;
import net.nyana.fluid.api.storage.FluidStorage;
import net.nyana.fluid.api.transaction.SnapshotJournal;
import net.nyana.fluid.api.transaction.TransactionContext;
import net.nyana.fluid.plugin.platform.DefaultVanillaBridge;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;

final class CauldronStorage extends SnapshotJournal<BlockData> implements FluidStorage, FluidSlotView {
    private static final FluidVariant WATER = FluidVariant.of(DefaultVanillaBridge.WATER);
    private static final FluidVariant LAVA = FluidVariant.of(DefaultVanillaBridge.LAVA);

    static @Nullable CauldronStorage at(ExistingBlock existing) {
        if (!(existing.world().platformWorld() instanceof org.bukkit.World world)) return null;
        return new CauldronStorage(world.getBlockAt(existing.x(), existing.y(), existing.z()));
    }

    private final Block block;

    private CauldronStorage(Block block) {
        this.block = block;
    }

    @Override
    public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        Preconditions.checkResource(resource, maxAmount);
        if (maxAmount < FluidAmount.BUCKET || this.block.getType() != Material.CAULDRON) return 0;
        if (resource.equals(CauldronStorage.WATER)) {
            super.updateSnapshots(transaction);
            BlockData data = Material.WATER_CAULDRON.createBlockData();
            if (data instanceof Levelled levelled) levelled.setLevel(levelled.getMaximumLevel());
            this.block.setBlockData(data, false);
            return FluidAmount.BUCKET;
        }
        if (resource.equals(CauldronStorage.LAVA)) {
            super.updateSnapshots(transaction);
            this.block.setType(Material.LAVA_CAULDRON, false);
            return FluidAmount.BUCKET;
        }
        return 0;
    }

    @Override
    public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        Preconditions.checkResource(resource, maxAmount);
        if (maxAmount < FluidAmount.BUCKET || !resource.equals(this.variant()) || this.amount() < FluidAmount.BUCKET)
            return 0;
        super.updateSnapshots(transaction);
        this.block.setType(Material.CAULDRON, false);
        return FluidAmount.BUCKET;
    }

    @Override
    public FluidVariant variant() {
        return switch (this.block.getType()) {
            case WATER_CAULDRON -> CauldronStorage.WATER;
            case LAVA_CAULDRON -> CauldronStorage.LAVA;
            default -> FluidVariant.blank();
        };
    }

    @Override
    public long amount() {
        return switch (this.block.getType()) {
            case WATER_CAULDRON -> this.block.getBlockData() instanceof Levelled levelled
                                   ? FluidAmount.BUCKET * levelled.getLevel() / levelled.getMaximumLevel() : 0;
            case LAVA_CAULDRON -> FluidAmount.BUCKET;
            default -> 0;
        };
    }

    @Override
    public long capacity() {
        return FluidAmount.BUCKET;
    }

    @Override
    public Iterator<FluidSlotView> iterator() {
        return List.<FluidSlotView>of(this).iterator();
    }

    @Override
    protected BlockData createSnapshot() {
        return this.block.getBlockData().clone();
    }

    @Override
    protected void revertToSnapshot(BlockData snapshot) {
        this.block.setBlockData(snapshot, false);
    }

    @Override
    protected void onRootCommit(BlockData originalState) {
        BlockData now = this.block.getBlockData();
        if (!now.equals(originalState)) this.block.setBlockData(now, true);
    }

    @Override
    protected void checkAccess() {
        Regions.checkChunk(this.block.getWorld(), this.block.getX() >> 4, this.block.getZ() >> 4);
    }
}
