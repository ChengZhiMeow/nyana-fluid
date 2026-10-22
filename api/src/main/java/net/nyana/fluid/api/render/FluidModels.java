package net.nyana.fluid.api.render;

import io.papermc.paper.datacomponent.DataComponentTypes;
import net.momirealms.craftengine.core.util.Key;
import net.nyana.fluid.api.fluid.FluidAttributes;
import net.nyana.fluid.api.fluid.FluidVariant;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * NyanaFluid 生成的模型和流体的渲染贴图。模型在 CE 生成资源包时一起生成。
 */
public final class FluidModels {
    /**
     * 流体没有设置贴图时用的贴图，按流体颜色染色。
     */
    public static final Key DEFAULT_TEXTURE = Key.of("minecraft", "block/water_still");
    /**
     * 内置的菜单液体格样式。
     */
    public static final Key FLUID_CELL = Key.of("nyana-fluid", "fluid_cell");
    private static final int WHITE = 0xFFFFFFFF;

    /**
     * 储罐里某种流体 {@code height} 像素高（1..16）的生成模型。
     */
    public static Key tankFluid(Key fluid, int height) {
        return Key.of("nyana-fluid", "tank_fluid/" + fluid.namespace() + "/" + fluid.value() + "/" + height);
    }

    public static Key texture(FluidAttributes attributes) {
        return attributes.texture().orElse(FluidModels.DEFAULT_TEXTURE);
    }

    /**
     * 贴图的染色，ARGB；不染色时是白色。
     */
    public static int tint(FluidAttributes attributes) {
        return attributes.tinted() ? FluidModels.color(attributes) : FluidModels.WHITE;
    }

    /**
     * 流体颜色，ARGB；没有设置时是白色。
     */
    public static int color(FluidAttributes attributes) {
        return attributes.color().isPresent() ? attributes.color().getAsInt() : FluidModels.WHITE;
    }

    static int color(FluidVariant fluid) {
        return fluid == null || fluid.isBlank() ? FluidModels.WHITE : FluidModels.color(fluid.attributes());
    }

    static int tint(FluidVariant fluid) {
        return fluid == null || fluid.isBlank() ? FluidModels.WHITE : FluidModels.tint(fluid.attributes());
    }

    /**
     * 一个只显示模型 {@code model} 的物品。
     */
    public static ItemStack item(Key model) {
        ItemStack stack = new ItemStack(Material.PAPER);
        stack.setData(DataComponentTypes.ITEM_MODEL, net.kyori.adventure.key.Key.key(model.namespace(), model.value()));
        return stack;
    }

    private FluidModels() {
    }
}
