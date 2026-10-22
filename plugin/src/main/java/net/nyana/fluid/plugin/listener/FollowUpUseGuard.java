package net.nyana.fluid.plugin.listener;

import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.nyana.fluid.api.internal.InteractionGuard;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class FollowUpUseGuard implements Listener {
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
        BukkitServerPlayer player = BukkitAdaptor.adapt(event.getPlayer());
        if (player == null) return;
        if (InteractionGuard.recent(event.getPlayer().getUniqueId(), player.gameTicks()) != null)
            event.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        InteractionGuard.forget(event.getPlayer().getUniqueId());
    }
}
