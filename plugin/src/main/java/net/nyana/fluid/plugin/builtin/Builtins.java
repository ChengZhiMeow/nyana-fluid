package net.nyana.fluid.plugin.builtin;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.Key;
import net.nyana.fluid.api.block.FluidBlockEntity;
import net.nyana.fluid.api.fluid.FluidAttributes;
import net.nyana.fluid.api.fluid.FluidSoundEvent;
import net.nyana.fluid.api.internal.Core;
import net.nyana.fluid.api.item.BucketSpec;
import net.nyana.fluid.api.item.ContainerSpec;
import net.nyana.fluid.api.item.ItemAccess;
import net.nyana.fluid.api.item.ItemFluidStorage;
import net.nyana.fluid.api.lookup.ControllerProvider;
import net.nyana.fluid.api.lookup.FluidLookups;
import net.nyana.fluid.api.platform.VanillaBridge;
import net.nyana.fluid.api.storage.FluidStorage;
import net.nyana.fluid.api.storage.base.CombinedFluidStorage;
import net.nyana.fluid.plugin.platform.DefaultVanillaBridge;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class Builtins {
    public static void register(Plugin plugin, VanillaBridge vanilla) {
        Builtins.registerVanillaFluids(plugin);
        for (BucketSpec spec : vanilla.containers()) Core.get().containers.registerBucket(plugin, spec);
        FluidLookups.BLOCK.registerForBlocks(plugin, (block, blockEntity, side) -> CauldronStorage.at(block),
                Key.of("minecraft", "cauldron"), Key.of("minecraft", "water_cauldron"), Key.of("minecraft", "lava_cauldron"));
        FluidLookups.BLOCK.registerForController(plugin, FluidBlockEntity.class, new FluidBlockEntities());
        FluidLookups.ITEM.registerForSetting(plugin, ContainerSpec.SETTING,
                (spec, item, access) -> new ItemFluidStorage(access != null ? access : ItemAccess.detached(item), spec));
    }

    private static void registerVanillaFluids(Plugin plugin) {
        Core.get().fluids.batch(plugin, batch -> batch
                .register(DefaultVanillaBridge.WATER, FluidAttributes.builder()
                        .displayName(Component.translatable("block.minecraft.water")).color(0xFF3F76E4)
                        .sound(FluidSoundEvent.FILL, Key.of("minecraft", "item.bucket.fill"))
                        .sound(FluidSoundEvent.EMPTY, Key.of("minecraft", "item.bucket.empty")).build())
                .register(DefaultVanillaBridge.LAVA, FluidAttributes.builder()
                        .displayName(Component.translatable("block.minecraft.lava")).color(0xFFFF6A00).luminance(15)
                        .texture(Key.of("minecraft", "block/lava_still")).tinted(false)
                        .temperature(1300).density(3000).viscosity(6000)
                        .sound(FluidSoundEvent.FILL, Key.of("minecraft", "item.bucket.fill_lava"))
                        .sound(FluidSoundEvent.EMPTY, Key.of("minecraft", "item.bucket.empty_lava")).build()));
        Core.get().fluids.tags().register(plugin, Key.of("minecraft", "water"), Set.of(DefaultVanillaBridge.WATER));
        Core.get().fluids.tags().register(plugin, Key.of("minecraft", "lava"), Set.of(DefaultVanillaBridge.LAVA));
    }

    private Builtins() {
    }

    private static final class FluidBlockEntities implements ControllerProvider<FluidBlockEntity, FluidStorage, Direction> {
        @Override
        public @Nullable FluidStorage find(FluidBlockEntity controller, @Nullable Direction side) {
            return controller.fluidStorage(side);
        }

        @Override
        public @Nullable FluidStorage findAll(List<FluidBlockEntity> controllers, @Nullable Direction side) {
            List<FluidStorage> storages = new ArrayList<>(controllers.size());
            for (FluidBlockEntity controller : controllers) {
                FluidStorage storage = controller.fluidStorage(side);
                if (storage != null) storages.add(storage);
            }
            if (storages.isEmpty()) return null;
            return storages.size() == 1 ? storages.get(0) : new CombinedFluidStorage(storages);
        }
    }
}
