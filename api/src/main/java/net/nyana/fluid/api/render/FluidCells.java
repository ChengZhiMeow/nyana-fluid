package net.nyana.fluid.api.render;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
import net.momirealms.craftengine.core.util.Key;
import net.nyana.fluid.api.fluid.FluidStack;
import org.bukkit.Color;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * 菜单里由多个格子拼成的液罐，多种流体从下往上叠放（类似匠魂冶炼炉）。每个格子是一个物品，
 * 每层的液面高度、染色和流体 id 写在 custom_model_data 里：设置了贴图的流体用自己的贴图，其余用样式贴图按流体颜色染色。
 * 样式在 CE 配置 {@code nyana-fluid:fluid_cells} 里定义，内置 {@link FluidModels#FLUID_CELL}。
 */
public final class FluidCells {
    /**
     * 一格的像素高度。
     */
    public static final int CELL_PIXELS = 18;
    /**
     * 一格里最多叠几层。
     */
    public static final int MAX_LAYERS = 10;

    public static ItemStack[][] cells(int columns, int rows, List<FluidStack> layers, long capacity) {
        return FluidCells.cells(FluidModels.FLUID_CELL, columns, rows, layers, capacity);
    }

    /**
     * @param layers   从下往上的各层流体
     * @param capacity 整个液罐的容量
     * @return {@code [row][column]}，第 0 行是最上面一行
     */
    public static ItemStack[][] cells(Key style, int columns, int rows, List<FluidStack> layers, long capacity) {
        float[] tops = FluidCells.tops(rows, layers, capacity);
        ItemStack[][] cells = new ItemStack[rows][columns];
        for (int row = 0; row < rows; row++) {
            float bottom = (rows - 1 - row) * FluidCells.CELL_PIXELS;
            CustomModelData.Builder data = CustomModelData.customModelData();
            for (int i = 0; i < FluidCells.MAX_LAYERS; i++) {
                float height = i < tops.length ? Math.max(0, Math.min(FluidCells.CELL_PIXELS, tops[i] - bottom)) : 0;
                data.addFloat(height);
                data.addColor(Color.fromARGB(i < tops.length ? FluidModels.tint(layers.get(i).variant()) : 0xFFFFFFFF));
                data.addString(i < tops.length ? layers.get(i).variant().fluidKey().toString() : "");
            }
            CustomModelData built = data.build();
            for (int column = 0; column < columns; column++) {
                ItemStack cell = FluidModels.item(style);
                cell.setData(DataComponentTypes.CUSTOM_MODEL_DATA, built);
                cells[row][column] = cell;
            }
        }
        return cells;
    }

    /**
     * 第 {@code row} 行（从上往下数）里占得最多的是哪一层；没有流体时返回 -1。
     */
    public static int layerAt(int row, int rows, List<FluidStack> layers, long capacity) {
        float[] tops = FluidCells.tops(rows, layers, capacity);
        float bottom = (rows - 1 - row) * FluidCells.CELL_PIXELS;
        float top = bottom + FluidCells.CELL_PIXELS;
        int best = -1;
        float bestSpan = 0;
        float below = 0;
        for (int i = 0; i < tops.length; i++) {
            float span = FluidCells.clamp(tops[i], bottom, top) - FluidCells.clamp(below, bottom, top);
            if (span > bestSpan) {
                bestSpan = span;
                best = i;
            }
            below = tops[i];
        }
        return best;
    }

    private static float[] tops(int rows, List<FluidStack> layers, long capacity) {
        int count = Math.min(layers.size(), FluidCells.MAX_LAYERS);
        float total = rows * FluidCells.CELL_PIXELS;
        float[] tops = new float[count];
        float top = 0;
        for (int i = 0; i < count; i++) {
            long amount = layers.get(i).amount();
            if (amount > 0 && capacity > 0) top += Math.max(1, (float) amount / capacity * total);
            tops[i] = Math.min(total, top);
        }
        return tops;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private FluidCells() {
    }
}
