package net.nyana.example.tank;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviors;
import net.momirealms.craftengine.core.util.Key;
import net.nyana.example.tank.block.ExampleTankBehavior;
import net.nyana.example.tank.listener.TankMenuListener;
import net.nyana.example.tank.menu.type.MenuType;
import net.nyana.fluid.api.NyanaFluid;
import net.nyana.fluid.api.fluid.FluidAmount;
import net.nyana.fluid.api.fluid.FluidAttributes;
import net.nyana.fluid.api.fluid.FluidRarity;
import net.nyana.fluid.api.fluid.FluidSoundEvent;
import net.nyana.fluid.api.item.BucketSpec;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {
    public static final Key HONEY = Key.of("example_plugin", "honey");

    @Override
    public void onLoad() {
        BlockBehaviors.register(Key.of("example_plugin", "fluid_tank"), (block, section) -> new ExampleTankBehavior(block, MenuType.SINGLE, section));
        BlockBehaviors.register(Key.of("example_plugin", "mixed_tank"), (block, section) -> new ExampleTankBehavior(block, MenuType.MIXED, section));
    }

    @Override
    public void onEnable() {
        NyanaFluid.fluids().register(this, Main.HONEY, FluidAttributes.builder()
                .displayName(Component.text("蜂蜜"))
                .color(0xFFFFB300)
                .density(1400)
                .viscosity(4000)
                .rarity(FluidRarity.UNCOMMON)
                .displayItem(Key.of("minecraft", "honey_bottle"))
                .texture(Key.of("minecraft", "block/honey_block_top"))
                .tinted(false)
                .sound(FluidSoundEvent.FILL, Key.of("minecraft", "item.bottle.fill"))
                .sound(FluidSoundEvent.EMPTY, Key.of("minecraft", "block.honey_block.place"))
                .build());

        NyanaFluid.containers().registerBucket(this, BucketSpec.builder()
                .emptyItem(Key.of("minecraft", "bucket"))
                .fullItem(Key.of("example_plugin", "honey_bucket"))
                .fluid(Main.HONEY)
                .amount(FluidAmount.BUCKET)
                .build());
        NyanaFluid.containers().registerBucket(this, BucketSpec.builder()
                .emptyItem(Key.of("minecraft", "glass_bottle"))
                .fullItem(Key.of("minecraft", "honey_bottle"))
                .fluid(Main.HONEY)
                .amount(FluidAmount.BOTTLE)
                .build());
        super.getServer().getPluginManager().registerEvents(new TankMenuListener(this), this);
    }
}
