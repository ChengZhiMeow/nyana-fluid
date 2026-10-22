package net.nyana.example.tank.listener;

import net.nyana.example.tank.menu.TankMenu;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.Set;

public final class TankMenuListener implements Listener {
    private static final Set<InventoryAction> PLACING = Set.of(InventoryAction.PLACE_ALL, InventoryAction.PLACE_ONE, InventoryAction.PLACE_SOME,
            InventoryAction.SWAP_WITH_CURSOR, InventoryAction.HOTBAR_SWAP);
    private final Plugin plugin;

    public TankMenuListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder(false) instanceof TankMenu menu) || !(event.getWhoClicked() instanceof Player player))
            return;
        int slot = event.getRawSlot();
        if (slot >= top.getSize()) {
            if (!event.isShiftClick()) return;
            event.setCancelled(true);
            ItemStack moving = event.getCurrentItem();
            ItemStack input = top.getItem(TankMenu.INPUT);
            if (moving == null || moving.isEmpty() || (input != null && !input.isEmpty())) return;
            top.setItem(TankMenu.INPUT, moving.clone());
            event.setCurrentItem(null);
            this.process(player, menu);
            return;
        }
        if (slot == TankMenu.INPUT) {
            this.process(player, menu);
            return;
        }
        if (slot == TankMenu.OUTPUT) {
            if (TankMenuListener.PLACING.contains(event.getAction())) event.setCancelled(true);
            return;
        }
        event.setCancelled(true);
        menu.click(player, slot);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder(false) instanceof TankMenu menu) || !(event.getWhoClicked() instanceof Player player))
            return;
        for (int slot : event.getRawSlots()) {
            if (slot < top.getSize() && slot != TankMenu.INPUT) {
                event.setCancelled(true);
                return;
            }
        }
        this.process(player, menu);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder(false) instanceof TankMenu menu && event.getPlayer() instanceof Player player)
            menu.close(player);
    }

    private void process(Player player, TankMenu menu) {
        player.getScheduler().run(this.plugin, task -> menu.processInput(), null);
    }
}
