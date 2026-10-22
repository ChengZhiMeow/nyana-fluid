package net.nyana.fluid.api.item;

import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.nyana.fluid.api.internal.ItemAccesses;
import net.nyana.fluid.api.lookup.FluidLookups;
import net.nyana.fluid.api.storage.FluidStorage;
import net.nyana.fluid.api.transaction.Transaction;
import net.nyana.fluid.api.transaction.TransactionContext;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.Nullable;

/**
 * 物品所在的位置，修改都参与事务。玩家位置放不下的物品提交后进背包或掉在脚下。
 */
public interface ItemAccess {
    /**
     * 玩家右键时用，和 {@link #ofHand(Player, InteractionHand)} 一样：所有游戏模式都真实交换手上的容器，
     * 满桶倒空后换成空桶，空桶装满后换成满桶，创造模式也一样。要原版创造模式「无限材料」的效果，用 {@link #ofCreative}。
     */
    static ItemAccess ofInteraction(Player player, InteractionHand hand) {
        return ItemAccesses.interaction(player, hand);
    }

    /**
     * 玩家手上的物品。创造模式也会真实扣减。
     */
    static ItemAccess ofHand(Player player, InteractionHand hand) {
        return ItemAccesses.hand(player, hand);
    }

    /**
     * 玩家光标上的物品（打开容器界面时）。
     */
    static ItemAccess ofCursor(Player player) {
        return ItemAccesses.cursor(player);
    }

    /**
     * 玩家背包的某个槽。
     */
    static ItemAccess ofSlot(Player player, int slot) {
        return ItemAccesses.slot(player, slot);
    }

    /**
     * 任意 Bukkit 背包的某个槽（漏斗、机器）。CE 没有背包的包装类型，所以这里用 Bukkit 的 {@link Inventory}。
     */
    static ItemAccess ofInventory(Inventory inventory, int slot) {
        return ItemAccesses.inventory(inventory, slot);
    }

    /**
     * 创造模式的无限材料：抽取不扣减；插入时如果玩家背包里还没有同样的物品，就在提交后给一个。
     */
    static ItemAccess ofCreative(Player player, Item contents) {
        return ItemAccesses.creative(player, contents);
    }

    /**
     * 脱离任何背包的物品副本。
     */
    static DetachedItemAccess detached(Item item) {
        return ItemAccesses.detached(item);
    }

    /**
     * 当前物品的副本（含数量）。
     */
    Item item();

    int amount();

    @Nullable Player player();

    /**
     * 放入物品（数量以 {@code amount} 为准，忽略 {@code item} 自身的数量）。
     */
    int insert(Item item, int amount, TransactionContext transaction);

    /**
     * 取走物品。只有和当前物品相同（数量除外）时才能取走。
     */
    int extract(Item item, int amount, TransactionContext transaction);

    /**
     * 交换：取走 {@code amount} 个当前物品，再放入同样数量的 {@code newItem}，两步都成功才算数，否则返回 0、什么都不变。
     */
    default int exchange(Item newItem, int amount, TransactionContext transaction) {
        if (amount < 0) throw new IllegalArgumentException("Amount must not be negative");
        Item current = this.item();
        if (current.isEmpty() || amount == 0) return 0;
        try (Transaction nested = Transaction.open(transaction)) {
            int extracted = this.extract(current, amount, nested);
            if (extracted > 0 && this.insert(newItem, extracted, nested) == extracted) {
                nested.commit();
                return extracted;
            }
        }
        return 0;
    }

    /**
     * 一次只操作一个物品的视图：{@link #amount()} 最多为 1，{@link #extract} 每次最多取 1 个。
     * 「16 个空桶里拿一个装水」就靠它：交换出来的满桶会进背包，剩下 15 个空桶不动。
     */
    default ItemAccess oneByOne() {
        return ItemAccesses.oneByOne(this);
    }

    /**
     * 这个位置上的物品作为流体存储：等价于 {@code FluidLookups.ITEM.find(item(), this)}。
     */
    default @Nullable FluidStorage fluidStorage() {
        return FluidLookups.ITEM.find(this.item(), this);
    }
}
