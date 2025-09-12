package com.github.nickxgrom.traceBrush.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.Objects;
import java.util.UUID;

import static com.github.nickxgrom.traceBrush.utils.TraceBrushUtils.isBrushInHand;
import static com.github.nickxgrom.traceBrush.utils.TraceBrushUtils.secondsToTicks;

import com.github.nickxgrom.traceBrush.TraceBrush;

public class UseTraceBrush implements Listener {
    private final TraceBrush plugin = JavaPlugin.getPlugin(TraceBrush.class);
    private final int RUB_TIME_IN_SECONDS = plugin.getConfig().getInt("rubTimeInSeconds");
    private final int MAX_TARGET_DISTANCE = plugin.getConfig().getInt("maxTargetDistance");
    private final long HOLD_TIMEOUT_MS = 200;

    @EventHandler
    public void onBrushUse(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();

        if (!isBrushInHand(player)) return;
        if (!(event.getRightClicked() instanceof Player target)) return;

        UUID playerId = player.getUniqueId();
        UUID targetId = target.getUniqueId();
        long now = System.currentTimeMillis();

        if (plugin.activeTraces.containsKey(playerId)) {
            if (plugin.activeTraces.get(playerId).equals(targetId)) {
                plugin.playersHoldingRightClickTimestamp.put(playerId, now);
            }

            return;
        }

        plugin.activeTraces.put(playerId, targetId);
        plugin.playersHoldingRightClickTimestamp.put(playerId, now);

        new BukkitRunnable() {
            final int maxTicks = secondsToTicks(RUB_TIME_IN_SECONDS);
            int ticks = 0;

            @Override
            public void run() {
                ticks++;

                boolean isPlayerInProgress = plugin.activeTraces.containsKey(playerId);
                boolean isKeyPressed = plugin.playersHoldingRightClickTimestamp.get(playerId) != null && System.currentTimeMillis() - plugin.playersHoldingRightClickTimestamp.get(playerId) <= HOLD_TIMEOUT_MS;
                boolean isLookingAtTarget = player.getTargetEntity(MAX_TARGET_DISTANCE) != null && Objects.requireNonNull(player.getTargetEntity(3)).getUniqueId().equals(targetId);

                if (!isPlayerInProgress || !isKeyPressed || !isLookingAtTarget || !isBrushInHand(player)) {
                    cleanup();
                    return;
                }


                if (ticks >= maxTicks) {
                    player.sendMessage("Trace finished. Player name: " + Bukkit.getOfflinePlayer(targetId).getName());
                    cleanup();
                }
            }

            private void cleanup() {
                plugin.activeTraces.remove(playerId);
                plugin.playersHoldingRightClickTimestamp.remove(playerId);
                cancel();
            }
        }.runTaskTimer(plugin, 0, 1);
    }
}
