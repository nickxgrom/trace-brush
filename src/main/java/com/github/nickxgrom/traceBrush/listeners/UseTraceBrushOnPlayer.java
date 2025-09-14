package com.github.nickxgrom.traceBrush.listeners;

import com.github.nickxgrom.traceBrush.TraceBrush;
import com.github.nickxgrom.traceBrush.utils.TraceBrushUtils;
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

public class UseTraceBrushOnPlayer implements Listener {
    private final TraceBrush plugin = JavaPlugin.getPlugin(TraceBrush.class);
    private final int RUB_TIME_IN_SECONDS = plugin.getConfig().getInt("rubTimeInSeconds");
    private final int MAX_TARGET_DISTANCE = plugin.getConfig().getInt("maxTargetDistance");
    private final long HOLD_TIMEOUT_MS = 200;

    @EventHandler
    public void onBrushUse(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();

        if (!isBrushInHand(player) || !TraceBrushUtils.isBrushHasFingerprint(player)) return;
        if (!(event.getRightClicked() instanceof Player target)) return;

        UUID playerId = player.getUniqueId();
        UUID targetId = target.getUniqueId();
        long now = System.currentTimeMillis();

        if (plugin.activePlayerTraces.containsKey(playerId)) {
            if (plugin.activePlayerTraces.get(playerId).equals(targetId)) {
                plugin.playersHoldingRightClickTimestamp.put(playerId, now);
            }

            return;
        }

        plugin.activePlayerTraces.put(playerId, targetId);
        plugin.playersHoldingRightClickTimestamp.put(playerId, now);

        new BukkitRunnable() {
            final int maxTicks = secondsToTicks(RUB_TIME_IN_SECONDS);
            int ticks = 0;

            @Override
            public void run() {
                ticks++;

                boolean isPlayerInProgress = plugin.activePlayerTraces.containsKey(playerId);
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
                plugin.activePlayerTraces.remove(playerId);
                plugin.playersHoldingRightClickTimestamp.remove(playerId);
                cancel();
            }
        }.runTaskTimer(plugin, 0, 1);
    }
}
