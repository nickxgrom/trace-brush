package com.github.nickxgrom.traceBrush.listeners;

import com.github.nickxgrom.traceBrush.CoreProtectHook;
import com.github.nickxgrom.traceBrush.TraceBrush;
import com.github.nickxgrom.traceBrush.models.TraceBrushItem;
import com.github.nickxgrom.traceBrush.models.types.CoreProtectInteractionType;
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
import java.util.UUID;

import static com.github.nickxgrom.traceBrush.utils.TraceBrushUtils.isBrushInHand;
import static com.github.nickxgrom.traceBrush.utils.TraceBrushUtils.secondsToTicks;

public class UseTraceBrushOnBlock implements Listener {
    private final TraceBrush plugin = JavaPlugin.getPlugin(TraceBrush.class);
    private final int RUB_TIME_IN_SECONDS = plugin.getConfig().getInt("rubTimeInSeconds");
    private final int MAX_TARGET_DISTANCE = plugin.getConfig().getInt("maxTargetDistance");
    private final CoreProtectAPI coreProtectAPI = new CoreProtectHook().getCoreProtectAPI();


    @EventHandler
    public void onBrushUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!isBrushInHand(player) || TraceBrushUtils.isBrushHasFingerprint(player)) return;
        if (event.getClickedBlock() == null) return;

        UUID playerId = player.getUniqueId();
        Block targetBlock = event.getClickedBlock();

        if (plugin.activeBlockTraces.containsKey(playerId)) return;

        plugin.activeBlockTraces.put(playerId, targetBlock.getLocation());

        new BukkitRunnable() {
            final int maxTicks = secondsToTicks(RUB_TIME_IN_SECONDS);
            int ticks = 0;
            Block currentTarget = targetBlock;

            @Override
            public void run() {
                if (!player.isHandRaised() || !isBrushInHand(player)) {
                    cleanup();
                    return;
                }

                Block targetBlockExact = player.getTargetBlockExact(MAX_TARGET_DISTANCE);

                if (targetBlockExact == null) {
                    cleanup();
                    return;
                }

                if (!targetBlockExact.getLocation().equals(currentTarget.getLocation())) {
                    currentTarget = targetBlockExact;
                    ticks = 0;
                }

                ticks++;

                if (ticks >= maxTicks) {
                    List<String[]> lookup = coreProtectAPI.blockLookup(currentTarget, 0);

                    if (!lookup.isEmpty()) {
                        String placedBy = null;
                        for (String[] row : lookup) {
                            CoreProtectAPI.ParseResult result = coreProtectAPI.parseResult(row);

                            if (result.getActionId() == CoreProtectInteractionType.PLACEMENT.ordinal()) {
                                placedBy = result.getPlayer();
                                break;
                            }
                        }

                        if (placedBy == null) {
                            TraceBrushItem.writeFingerprintToBrush(player, null, null);
                            return;
                        }

                        TraceBrushItem.writeFingerprintToBrush(player, Bukkit.getOfflinePlayer(placedBy).getUniqueId(), currentTarget);
                    } else {
                        TraceBrushItem.writeFingerprintToBrush(player, null, null);
                    }

                    cleanup();
                }
            }

            private void cleanup() {
                plugin.activeBlockTraces.remove(playerId);
                cancel();
            }
        }.runTaskTimer(plugin, 0, 1);
    }
}
