package net.nyana.fluid.api.recipe;

import net.momirealms.craftengine.core.util.Key;
import net.nyana.fluid.api.fluid.FluidVariant;
import net.nyana.fluid.api.fluid.component.FluidComponents;
import net.nyana.fluid.api.internal.Ingredients;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

/**
 * 流体原料。配置写法：{@code minecraft:water}、{@code "#tag"}、列表（任一）、
 * {@code {fluid: .., components: {..}}}、{@code {type: .., ...}}（{@link FluidIngredientType} 注册的种类）。
 */
public interface FluidIngredient extends Predicate<FluidVariant> {
    /**
     * 单一流体（忽略组件）。
     */
    static FluidIngredient of(Key fluid) {
        return new Ingredients.Single(fluid);
    }

    static FluidIngredient tag(Key tag) {
        return new Ingredients.TagIngredient(tag);
    }

    /**
     * 任一：满足其中一个即可。
     */
    static FluidIngredient anyOf(FluidIngredient... options) {
        return FluidIngredient.anyOf(Arrays.asList(options));
    }

    /**
     * 任一：满足其中一个即可。
     */
    static FluidIngredient anyOf(List<? extends FluidIngredient> options) {
        return new Ingredients.AnyOf(List.copyOf(options));
    }

    /**
     * 在 {@code base} 的基础上，还要求变体带有 {@code required} 里的全部组件且值相等。
     */
    static FluidIngredient withComponents(FluidIngredient base, FluidComponents required) {
        return new Ingredients.WithComponents(base, required);
    }

    static FluidIngredient empty() {
        return Ingredients.EMPTY;
    }

    /**
     * 从配置值解析原料，写法见类说明。
     *
     * @throws IllegalArgumentException 写法不合法
     */
    static FluidIngredient parse(Object value) {
        return Ingredients.parse(value);
    }

    FluidIngredientType<?> type();

    /**
     * 变体是否符合要求。空变体永远不符合。
     */
    @Override
    boolean test(FluidVariant variant);

    /**
     * 用于展示的若干变体，例如 JEI 式的轮播。
     */
    List<FluidVariant> displayVariants();
}
