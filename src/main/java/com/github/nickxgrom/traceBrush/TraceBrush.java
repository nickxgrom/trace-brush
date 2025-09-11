package com.github.nickxgrom.traceBrush;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class TraceBrush extends JavaPlugin implements Listener {
    private static final long HOLD_TIMEOUT_MS = 200;
    //    TODO move to config
    final int RUB_TIME_IN_SECONDS = 2;
    final int MAX_TARGET_DISTANCE = 3;

    Map<UUID, UUID> activeTraces = new HashMap<>();
    Map<UUID, Long> playersHoldingRightClickTimestamp = new HashMap<>();

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onBrushUse(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();

        if (!(event.getRightClicked() instanceof Player target)) return;
        if (!isBrushInHand(player)) return;

        UUID playerId = player.getUniqueId();
        UUID targetId = target.getUniqueId();
        long now = System.currentTimeMillis();

        if (activeTraces.containsKey(playerId)) {
            if (activeTraces.get(playerId).equals(targetId)) {
                playersHoldingRightClickTimestamp.put(playerId, now);
            }

            return;
        }

        activeTraces.put(playerId, targetId);
        playersHoldingRightClickTimestamp.put(playerId, now);

        new BukkitRunnable() {
            final int maxTicks = secondsToTicks(RUB_TIME_IN_SECONDS);
            int ticks = 0;

            @Override
            public void run() {
                ticks++;

                boolean isPlayerInProgress = activeTraces.containsKey(playerId);
                boolean isKeyPressed = playersHoldingRightClickTimestamp.get(playerId) != null && System.currentTimeMillis() - playersHoldingRightClickTimestamp.get(playerId) <= HOLD_TIMEOUT_MS;
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
                activeTraces.remove(playerId);
                playersHoldingRightClickTimestamp.remove(playerId);
                cancel();
            }
        }.runTaskTimer(this, 0, 1);
    }


    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    private boolean isBrushInHand(Player player) {
        return player.getInventory().getItemInMainHand().getType() == Material.BRUSH ||
                player.getInventory().getItemInOffHand().getType() == Material.BRUSH;
    }

    private int secondsToTicks(int seconds) {
        return seconds * 20;
    }
}
