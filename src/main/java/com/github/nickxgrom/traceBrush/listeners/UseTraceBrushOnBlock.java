package com.github.nickxgrom.traceBrush.listeners;

import com.github.nickxgrom.traceBrush.CoreProtectHook;
import com.github.nickxgrom.traceBrush.TraceBrush;
import com.github.nickxgrom.traceBrush.models.TraceBrushItem;
import com.github.nickxgrom.traceBrush.utils.TraceBrushUtils;
import net.coreprotect.CoreProtectAPI;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.github.nickxgrom.traceBrush.utils.TraceBrushUtils.isBrushInHand;
import static com.github.nickxgrom.traceBrush.utils.TraceBrushUtils.secondsToTicks;

public class UseTraceBrushOnBlock implements Listener {
    private final TraceBrush plugin = JavaPlugin.getPlugin(TraceBrush.class);
    private final int RUB_TIME_IN_SECONDS = plugin.getConfig().getInt("rubTimeInSeconds");
    private final int MAX_TARGET_DISTANCE = plugin.getConfig().getInt("maxTargetDistance");
    private final long HOLD_TIMEOUT_MS = 200;
    private final CoreProtectAPI coreProtectAPI = new CoreProtectHook().getCoreProtectAPI();


    @EventHandler
    public void onBrushUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!isBrushInHand(player) || TraceBrushUtils.isBrushHasFingerprint(player)) return;
        if (event.getClickedBlock() == null) return;

        UUID playerId = player.getUniqueId();
        Block targetBlock = event.getClickedBlock();
        long now = System.currentTimeMillis();

        if (plugin.activeBlockTraces.containsKey(playerId)) {
            if (plugin.activeBlockTraces.get(playerId).equals(targetBlock.getLocation())) {
                plugin.playersHoldingRightClickTimestamp.put(playerId, now);
            }

            return;
        }

        plugin.activeBlockTraces.put(playerId, targetBlock.getLocation());
        plugin.playersHoldingRightClickTimestamp.put(playerId, now);

        new BukkitRunnable() {
            final int maxTicks = secondsToTicks(RUB_TIME_IN_SECONDS);
            int ticks = 0;

            @Override
            public void run() {
                ticks++;

                boolean isPlayerInProgress = plugin.activeBlockTraces.containsKey(playerId);
                boolean isKeyPressed = plugin.playersHoldingRightClickTimestamp.get(playerId) != null && System.currentTimeMillis() - plugin.playersHoldingRightClickTimestamp.get(playerId) <= HOLD_TIMEOUT_MS;
                boolean isLookingAtTarget;

                try {
                    isLookingAtTarget = Objects.requireNonNull(player.getTargetBlockExact(MAX_TARGET_DISTANCE)).getLocation().equals(targetBlock.getLocation());
                } catch (Exception e) {
                    isLookingAtTarget = false;
                }

                if (!isPlayerInProgress || !isKeyPressed || !isLookingAtTarget || !isBrushInHand(player)) {
                    cleanup();
                    return;
                }

                if (ticks >= maxTicks) {
                    List<String[]> lookup = coreProtectAPI.blockLookup(targetBlock, 0);

                    if (!lookup.isEmpty()) {
                        String[] lastEntry = lookup.getLast();
                        String placer = coreProtectAPI.parseResult(lastEntry).getPlayer();

                        TraceBrushItem.writeFingerprintToBrush(player, Bukkit.getOfflinePlayer(placer).getUniqueId(), targetBlock);
                    } else {
                        TraceBrushItem.writeFingerprintToBrush(player, null, null);
                    }

                    cleanup();
                }
            }

            private void cleanup() {
                plugin.activeBlockTraces.remove(playerId);
                plugin.playersHoldingRightClickTimestamp.remove(playerId);
                cancel();
            }
        }.runTaskTimer(plugin, 0, 1);
    }
}
