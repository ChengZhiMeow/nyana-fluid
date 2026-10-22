package net.nyana.fluid.api.internal;

import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.nyana.fluid.api.item.DetachedItemAccess;
import net.nyana.fluid.api.item.ItemAccess;
import net.nyana.fluid.api.transaction.SnapshotJournal;
import net.nyana.fluid.api.transaction.TransactionContext;
import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * {@link ItemAccess} implementations. Every slot write is journaled; overflow is delivered after the root commit.
 */
@ApiStatus.Internal
public final class ItemAccesses {
    private static final int OFF_HAND_SLOT = 40;

    public static ItemAccess interaction(Player player, InteractionHand hand) {
        // Swap the held container in every game mode; ItemAccess.ofCreative keeps vanilla's creative behaviour.
        return ItemAccesses.hand(player, hand);
    }

    public static ItemAccess hand(Player player, InteractionHand hand) {
        org.bukkit.entity.Player bukkit = ItemAccesses.bukkit(player);
        return new PlayerSlotAccess(player, bukkit, ItemAccesses.handSlot(bukkit, hand));
    }

    public static ItemAccess cursor(Player player) {
        return new CursorAccess(player, ItemAccesses.bukkit(player));
    }

    public static ItemAccess slot(Player player, int slot) {
        org.bukkit.entity.Player bukkit = ItemAccesses.bukkit(player);
        if (slot < 0 || slot >= bukkit.getInventory().getSize())
            throw new IndexOutOfBoundsException("Inventory slot " + slot);
        return new PlayerSlotAccess(player, bukkit, slot);
    }

    public static ItemAccess inventory(Inventory inventory, int slot) {
        Objects.requireNonNull(inventory, "inventory");
        if (slot < 0 || slot >= inventory.getSize()) throw new IndexOutOfBoundsException("Inventory slot " + slot);
        return new InventorySlotAccess(inventory, slot);
    }

    public static ItemAccess creative(Player player, Item contents) {
        return new CreativeAccess(player, ItemAccesses.bukkit(player), contents);
    }

    public static DetachedItemAccess detached(Item item) {
        return new DetachedAccess(item.isEmpty() ? null : ItemStacks.toBukkit(item));
    }

    public static ItemAccess oneByOne(ItemAccess access) {
        return access instanceof OneByOne ? access : new OneByOne(access);
    }

    private static org.bukkit.entity.Player bukkit(Player player) {
        Objects.requireNonNull(player, "player");
        if (player.platformPlayer() instanceof org.bukkit.entity.Player bukkit) return bukkit;
        throw new IllegalArgumentException("Not a Bukkit player: " + player);
    }

    private static int handSlot(org.bukkit.entity.Player player, InteractionHand hand) {
        return hand == InteractionHand.OFF_HAND ? ItemAccesses.OFF_HAND_SLOT : player.getInventory().getHeldItemSlot();
    }

    private static void checkCount(int amount) {
        if (amount < 0) throw new IllegalArgumentException("Amount must not be negative");
    }

    private static Consumer<List<ItemStack>> giveTo(org.bukkit.entity.Player player) {
        return stacks -> {
            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(stacks.toArray(new ItemStack[0]));
            if (leftovers.isEmpty()) return;
            Location location = player.getLocation();
            for (ItemStack leftover : leftovers.values()) player.getWorld().dropItemNaturally(location, leftover);
        };
    }

    private ItemAccesses() {
    }

    /**
     * Items that did not fit into the main slot. Delivered after the root commit, or kept for detached accesses.
     */
    static final class Overflow extends SnapshotJournal<List<ItemStack>> {
        private final List<ItemStack> pending = new ArrayList<>();
        private final @Nullable Consumer<List<ItemStack>> deliver;

        Overflow(@Nullable Consumer<List<ItemStack>> deliver) {
            this.deliver = deliver;
        }

        void add(ItemStack prototype, int count, TransactionContext transaction) {
            super.updateSnapshots(transaction);
            int max = Math.max(1, prototype.getMaxStackSize());
            while (count > 0) {
                int size = Math.min(count, max);
                ItemStack stack = prototype.clone();
                stack.setAmount(size);
                this.pending.add(stack);
                count -= size;
            }
        }

        List<ItemStack> pending() {
            List<ItemStack> copies = new ArrayList<>(this.pending.size());
            for (ItemStack stack : this.pending) copies.add(stack.clone());
            return copies;
        }

        @Override
        protected List<ItemStack> createSnapshot() {
            return new ArrayList<>(this.pending);
        }

        @Override
        protected void revertToSnapshot(List<ItemStack> snapshot) {
            this.pending.clear();
            this.pending.addAll(snapshot);
        }

        @Override
        protected void onRootCommit(List<ItemStack> originalState) {
            if (this.deliver == null || this.pending.isEmpty()) return;
            List<ItemStack> stacks = new ArrayList<>(this.pending);
            this.pending.clear();
            this.deliver.accept(stacks);
        }
    }

    private abstract static class SlotAccess extends SnapshotJournal<ItemStack> implements ItemAccess {
        abstract @Nullable ItemStack read();

        abstract void write(@Nullable ItemStack stack);

        abstract @Nullable Overflow overflow();

        @Override
        public Item item() {
            return ItemStacks.toCe(this.read());
        }

        @Override
        public int amount() {
            ItemStack stack = this.read();
            return ItemStacks.isEmpty(stack) ? 0 : stack.getAmount();
        }

        @Override
        public int insert(Item item, int amount, TransactionContext transaction) {
            ItemAccesses.checkCount(amount);
            if (amount == 0 || item.isEmpty()) return 0;
            ItemStack add = ItemStacks.toBukkit(item);
            ItemStack current = this.read();
            int inserted = 0;
            if (ItemStacks.isEmpty(current)) {
                int size = Math.min(amount, Math.max(1, add.getMaxStackSize()));
                this.updateSnapshots(transaction);
                ItemStack placed = add.clone();
                placed.setAmount(size);
                this.write(placed);
                inserted = size;
            } else if (current.isSimilar(add)) {
                int size = Math.min(amount, current.getMaxStackSize() - current.getAmount());
                if (size > 0) {
                    this.updateSnapshots(transaction);
                    ItemStack grown = current.clone();
                    grown.setAmount(current.getAmount() + size);
                    this.write(grown);
                    inserted = size;
                }
            }
            Overflow overflow = this.overflow();
            if (inserted < amount && overflow != null) {
                overflow.add(add, amount - inserted, transaction);
                inserted = amount;
            }
            return inserted;
        }

        @Override
        public int extract(Item item, int amount, TransactionContext transaction) {
            ItemAccesses.checkCount(amount);
            if (amount == 0 || item.isEmpty()) return 0;
            ItemStack current = this.read();
            if (ItemStacks.isEmpty(current) || !current.isSimilar(ItemStacks.toBukkit(item))) return 0;
            int taken = Math.min(amount, current.getAmount());
            this.updateSnapshots(transaction);
            ItemStack rest = current.clone();
            rest.setAmount(current.getAmount() - taken);
            this.write(rest.getAmount() == 0 ? null : rest);
            return taken;
        }

        @Override
        protected @Nullable ItemStack createSnapshot() {
            ItemStack stack = this.read();
            return ItemStacks.isEmpty(stack) ? null : stack.clone();
        }

        @Override
        protected void revertToSnapshot(@Nullable ItemStack snapshot) {
            this.write(snapshot);
        }
    }

    private static final class PlayerSlotAccess extends SlotAccess {
        private final Player player;
        private final org.bukkit.entity.Player bukkit;
        private final int slot;
        private final Overflow overflow;

        private PlayerSlotAccess(Player player, org.bukkit.entity.Player bukkit, int slot) {
            this.player = player;
            this.bukkit = bukkit;
            this.slot = slot;
            this.overflow = new Overflow(ItemAccesses.giveTo(bukkit));
        }

        @Override
        @Nullable ItemStack read() {
            return this.bukkit.getInventory().getItem(this.slot);
        }

        @Override
        void write(@Nullable ItemStack stack) {
            this.bukkit.getInventory().setItem(this.slot, stack);
        }

        @Override
        Overflow overflow() {
            return this.overflow;
        }

        @Override
        public Player player() {
            return this.player;
        }

        @Override
        protected void checkAccess() {
            Regions.checkEntity(this.bukkit);
        }
    }

    private static final class CursorAccess extends SlotAccess {
        private final Player player;
        private final org.bukkit.entity.Player bukkit;
        private final Overflow overflow;

        private CursorAccess(Player player, org.bukkit.entity.Player bukkit) {
            this.player = player;
            this.bukkit = bukkit;
            this.overflow = new Overflow(ItemAccesses.giveTo(bukkit));
        }

        @Override
        @Nullable ItemStack read() {
            return this.bukkit.getItemOnCursor();
        }

        @Override
        void write(@Nullable ItemStack stack) {
            this.bukkit.setItemOnCursor(stack);
        }

        @Override
        Overflow overflow() {
            return this.overflow;
        }

        @Override
        public Player player() {
            return this.player;
        }

        @Override
        protected void checkAccess() {
            Regions.checkEntity(this.bukkit);
        }
    }

    private static final class InventorySlotAccess extends SlotAccess {
        private final Inventory inventory;
        private final int slot;

        private InventorySlotAccess(Inventory inventory, int slot) {
            this.inventory = inventory;
            this.slot = slot;
        }

        @Override
        @Nullable ItemStack read() {
            return this.inventory.getItem(this.slot);
        }

        @Override
        void write(@Nullable ItemStack stack) {
            this.inventory.setItem(this.slot, stack);
        }

        @Override
        @Nullable Overflow overflow() {
            return null;
        }

        @Override
        public @Nullable Player player() {
            return null;
        }

        @Override
        protected void checkAccess() {
            Location location = this.inventory.getLocation();
            if (location != null && location.getWorld() != null) {
                Regions.checkChunk(location.getWorld(), location.getBlockX() >> 4, location.getBlockZ() >> 4);
            }
        }
    }

    private static final class DetachedAccess extends SlotAccess implements DetachedItemAccess {
        private final Overflow overflow = new Overflow(null);
        private @Nullable ItemStack stack;

        private DetachedAccess(@Nullable ItemStack stack) {
            this.stack = stack;
        }

        @Override
        @Nullable ItemStack read() {
            return this.stack;
        }

        @Override
        void write(@Nullable ItemStack stack) {
            this.stack = stack == null ? null : stack.clone();
        }

        @Override
        Overflow overflow() {
            return this.overflow;
        }

        @Override
        public @Nullable Player player() {
            return null;
        }

        @Override
        public List<Item> results() {
            List<Item> results = new ArrayList<>();
            if (!ItemStacks.isEmpty(this.stack)) results.add(ItemStacks.toCe(this.stack));
            for (ItemStack extra : this.overflow.pending()) results.add(ItemStacks.toCe(extra));
            return List.copyOf(results);
        }
    }

    private static final class CreativeAccess implements ItemAccess {
        private final Player player;
        private final org.bukkit.entity.Player bukkit;
        private final Item contents;
        private final Overflow overflow;

        private CreativeAccess(Player player, org.bukkit.entity.Player bukkit, Item contents) {
            this.player = player;
            this.bukkit = bukkit;
            this.contents = contents.copy();
            this.overflow = new Overflow(ItemAccesses.giveTo(bukkit));
        }

        @Override
        public Item item() {
            return this.contents.copy();
        }

        @Override
        public int amount() {
            return this.contents.isEmpty() ? 0 : this.contents.count();
        }

        @Override
        public Player player() {
            return this.player;
        }

        @Override
        public int insert(Item item, int amount, TransactionContext transaction) {
            ItemAccesses.checkCount(amount);
            if (amount == 0 || item.isEmpty()) return 0;
            ItemStack stack = ItemStacks.toBukkit(item);
            if (!this.bukkit.getInventory().containsAtLeast(stack, 1)) this.overflow.add(stack, 1, transaction);
            return amount;
        }

        @Override
        public int extract(Item item, int amount, TransactionContext transaction) {
            ItemAccesses.checkCount(amount);
            if (amount == 0 || this.contents.isEmpty()) return 0;
            return ItemStacks.toBukkit(this.contents).isSimilar(ItemStacks.toBukkit(item)) ? amount : 0;
        }
    }

    private static final class OneByOne implements ItemAccess {
        private final ItemAccess delegate;

        private OneByOne(ItemAccess delegate) {
            this.delegate = delegate;
        }

        @Override
        public Item item() {
            Item item = this.delegate.item();
            return item.isEmpty() ? item : item.copyWithCount(1);
        }

        @Override
        public int amount() {
            return Math.min(1, this.delegate.amount());
        }

        @Override
        public @Nullable Player player() {
            return this.delegate.player();
        }

        @Override
        public int insert(Item item, int amount, TransactionContext transaction) {
            return this.delegate.insert(item, amount, transaction);
        }

        @Override
        public int extract(Item item, int amount, TransactionContext transaction) {
            return this.delegate.extract(item, Math.min(amount, 1), transaction);
        }

        @Override
        public ItemAccess oneByOne() {
            return this;
        }
    }
}
