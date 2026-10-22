package net.nyana.fluid.api.lookup;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 按 CE 方块实体 controller 提供能力,.CE 允许一个方块挂多个行为（组合 controller）, 查找会收集
 * 同一类型的全部 controller（按 order 排列）交给 {@link #findAll(List, Object)},.
 */
@FunctionalInterface
public interface ControllerProvider<T, A, C> {
    /**
     * 从一个 controller 提供能力,.
     */
    @Nullable A find(T controller, @Nullable C context);

    /**
     * 从同一方块上的全部同类 controller 提供能力,.默认返回第一个非 {@code null} 的 {@link #find} 结果；
     * 需要把多个 controller 合并成一个能力时覆写它,.
     */
    default @Nullable A findAll(List<T> controllers, @Nullable C context) {
        for (T controller : controllers) {
            A found = this.find(controller, context);
            if (found != null) return found;
        }
        return null;
    }
}
