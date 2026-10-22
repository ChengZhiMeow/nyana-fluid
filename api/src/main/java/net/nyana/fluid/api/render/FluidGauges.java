package net.nyana.fluid.api.render;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
import net.momirealms.craftengine.core.util.Key;
import net.nyana.fluid.api.fluid.FluidVariant;
import net.nyana.fluid.api.internal.Core;
import org.bukkit.Color;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 菜单里显示液面的单个物品。每种流体在自己的配置 {@code gauges.<id>} 里定义它在这个液面里的样子
 * （{@code split} 把贴图切成若干档，{@code select} 逐档指定贴图、模型或物品），
 * 没有定义的流体用 {@code nyana-fluid:gauges.<id>} 的默认样式。
 */
public final class FluidGauges {
    private static final Key EMPTY = Key.of("minecraft", "air");

    /**
     * @param gauge 液面样式 id
     * @param fluid 显示的流体；{@code null} 或空表示空
     */
    public static ItemStack item(Key gauge, @Nullable FluidVariant fluid, long amount, long capacity) {
        boolean empty = fluid == null || fluid.isBlank() || amount <= 0 || capacity <= 0;
        float fraction = empty ? 0f : Math.max(1e-6f, Math.min(1f, (float) amount / capacity));
        Key model = Core.get().gauges.model(gauge, empty ? null : fluid.fluidKey(), fraction);
        ItemStack stack = FluidModels.item(model == null ? FluidGauges.EMPTY : model);
        stack.setData(DataComponentTypes.CUSTOM_MODEL_DATA, CustomModelData.customModelData()
                .addFloat(fraction)
                .addColor(Color.fromARGB(FluidModels.color(fluid)))
                .addString(empty ? "" : fluid.fluidKey().toString())
                .build());
        return stack;
    }

    private FluidGauges() {
    }
}
