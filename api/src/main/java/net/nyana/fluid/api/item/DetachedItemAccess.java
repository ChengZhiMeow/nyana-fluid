package net.nyana.fluid.api.item;

import net.momirealms.craftengine.core.item.Item;

import java.util.List;

/**
 * 脱离任何背包的物品位置。放不下的物品不会丢，而是收集起来，从 {@link #results()} 取出。
 */
public interface DetachedItemAccess extends ItemAccess {
    /**
     * 当前的全部结果：主物品（非空时）加上溢出的物品。在事务提交后读取。
     */
    List<Item> results();
}
