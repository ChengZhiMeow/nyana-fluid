package net.nyana.fluid.api.item;

import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.nyana.fluid.api.storage.FluidStorage;

/**
 * 自定义交互规则, 例如某些物品每次只取 250 mB,.按优先级从高到低询问, 第一个返回非 {@link InteractionResult#PASS}
 * 的结果生效；都返回 {@code PASS} 时执行默认规则,.
 */
@FunctionalInterface
public interface InteractionHandler {
    InteractionResult handle(ItemAccess item, FluidStorage target, InteractionContext context);
}
