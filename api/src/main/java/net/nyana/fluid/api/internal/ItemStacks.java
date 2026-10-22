package net.nyana.fluid.api.internal;

import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.item.BukkitItem;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * Conversions between CE {@link Item} and Bukkit {@link ItemStack}. Every conversion copies.
 */
@ApiStatus.Internal
public final class ItemStacks {
    public static boolean isEmpty(@Nullable ItemStack stack) {
        return stack == null || stack.getType().isAir() || stack.getAmount() <= 0;
    }

    /**
     * A copy of the CE item as a Bukkit stack.
     */
    public static ItemStack toBukkit(Item item) {
        if (item instanceof BukkitItem bukkit) return bukkit.getBukkitItem().clone();
        if (item.platformItem() instanceof ItemStack stack) return stack.clone();
        throw new IllegalArgumentException("Not a Bukkit-backed CraftEngine item: " + item);
    }

    /**
     * A CE item wrapping a copy of the Bukkit stack; air becomes an empty item.
     */
    public static Item toCe(@Nullable ItemStack stack) {
        return BukkitAdaptor.adapt(ItemStacks.isEmpty(stack) ? ItemStack.empty() : stack.clone());
    }

    /**
     * Builds one item by id: CE custom items through CE, vanilla ids through Bukkit materials as a fallback.
     */
    public static @Nullable Item create(Key id) {
        try {
            Item item = Item.byId(id);
            if (item != null && !item.isEmpty()) return item;
        } catch (RuntimeException ignored) {
            // CE not running, or the id is not a CE-buildable item: fall through to vanilla materials.
        }
        if (!id.isMinecraftNamespace()) return null;
        Material material = Material.matchMaterial(id.toString());
        if (material == null || !material.isItem() || material.isAir()) return null;
        return BukkitAdaptor.adapt(new ItemStack(material));
    }

    private ItemStacks() {
    }
}
