package net.nyana.fluid.api.render;

import net.momirealms.craftengine.core.util.Key;
import net.nyana.fluid.api.block.FluidTankController;
import net.nyana.fluid.api.fluid.FluidVariant;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 默认的流体展示：渲染设置里给这种流体配了 {@code model} 就用它（含 {@code {height}} 时按档换模型, 否则整体缩放）,
 * 否则用 NyanaFluid 生成的模型（流体贴图, 默认 {@code water_still} 按流体颜色染色）,.
 */
public class DefaultFluidTankDisplay extends FluidTankDisplay {
    private static final String HEIGHT = "{height}";

    public DefaultFluidTankDisplay(FluidTankController tank, TankRenderSpec spec) {
        super(tank, spec);
    }

    @Override
    protected @Nullable ItemStack model(FluidVariant fluid, int height) {
        String template = this.template(fluid);
        Key model = template == null ? FluidModels.tankFluid(fluid.fluidKey(), height) : Key.of(template.replace(DefaultFluidTankDisplay.HEIGHT, String.valueOf(height)));
        return FluidModels.item(model);
    }

    @Override
    protected boolean scalesModel(FluidVariant fluid) {
        String template = this.template(fluid);
        return template != null && !template.contains(DefaultFluidTankDisplay.HEIGHT);
    }

    private @Nullable String template(FluidVariant fluid) {
        return this.spec().fluid(fluid.fluidKey()).map(TankRenderSpec.FluidRender::model).orElse(null);
    }
}
