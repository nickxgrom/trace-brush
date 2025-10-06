package com.github.nickxgrom.traceBrush.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;

import static com.github.nickxgrom.traceBrush.utils.TraceBrushUtils.isBrushInHand;

public class OnSuspiciousBlockBrush implements Listener {
    @EventHandler
    public void onSuspiciousBlockBrush(EntityChangeBlockEvent event) {
        if ((event.getEntity() instanceof Player player)) {
            if (isBrushInHand(player, false)) {
                if (event.getBlock().getType() == Material.SUSPICIOUS_SAND || event.getBlock().getType() == Material.SUSPICIOUS_GRAVEL) {
                    event.setCancelled(true);
                }
            }
        }
    }
}
