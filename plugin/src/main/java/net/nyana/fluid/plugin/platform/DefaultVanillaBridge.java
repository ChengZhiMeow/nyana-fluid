package net.nyana.fluid.plugin.platform;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.Key;
import net.nyana.fluid.api.fluid.FluidAmount;
import net.nyana.fluid.api.internal.ItemStacks;
import net.nyana.fluid.api.item.BucketSpec;
import net.nyana.fluid.api.platform.VanillaBridge;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import java.util.List;

public final class DefaultVanillaBridge implements VanillaBridge {
    public static final Key WATER = Key.of("minecraft", "water");
    public static final Key LAVA = Key.of("minecraft", "lava");

    private static Item waterBottle() {
        ItemStack bottle = new ItemStack(Material.POTION);
        bottle.editMeta(PotionMeta.class, meta -> meta.setBasePotionType(PotionType.WATER));
        return ItemStacks.toCe(bottle);
    }

    private static boolean isWaterBottle(Item item) {
        ItemStack stack = ItemStacks.toBukkit(item);
        return stack.getItemMeta() instanceof PotionMeta meta && meta.getBasePotionType() == PotionType.WATER && !meta.hasCustomEffects();
    }

    @Override
    public List<BucketSpec> containers() {
        Key bucket = Key.of("minecraft", "bucket");
        return List.of(
                BucketSpec.builder().emptyItem(bucket).fullItem(Key.of("minecraft", "water_bucket")).fluid(DefaultVanillaBridge.WATER).amount(FluidAmount.BUCKET).build(),
                BucketSpec.builder().emptyItem(bucket).fullItem(Key.of("minecraft", "lava_bucket")).fluid(DefaultVanillaBridge.LAVA).amount(FluidAmount.BUCKET).build(),
                BucketSpec.builder().emptyItem(Key.of("minecraft", "glass_bottle"))
                        .fullItem(Key.of("minecraft", "potion"), DefaultVanillaBridge::waterBottle, DefaultVanillaBridge::isWaterBottle)
                        .fluid(DefaultVanillaBridge.WATER).amount(FluidAmount.BOTTLE)
                        .fillSound(Key.of("minecraft", "item.bottle.fill")).emptySound(Key.of("minecraft", "item.bottle.empty")).build());
    }
}
