package com.github.nickxgrom.traceBrush.listeners;

import com.github.nickxgrom.traceBrush.CoreProtectHook;
import com.github.nickxgrom.traceBrush.TraceBrush;
import com.github.nickxgrom.traceBrush.models.TraceBrushItem;
import com.github.nickxgrom.traceBrush.models.types.CoreProtectInteractionType;
import com.github.nickxgrom.traceBrush.utils.TraceBrushUtils;
import net.coreprotect.CoreProtectAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.UUID;

import static com.github.nickxgrom.traceBrush.utils.TraceBrushUtils.isBrushInHand;
import static com.github.nickxgrom.traceBrush.utils.TraceBrushUtils.secondsToTicks;

public class UseTraceBrushOnBlock implements Listener {
    private final TraceBrush plugin = JavaPlugin.getPlugin(TraceBrush.class);
    private final int RUB_TIME_IN_SECONDS = plugin.getConfig().getInt("rubTimeInSecondsForBlock", 3);
    private final int MAX_TARGET_DISTANCE = plugin.getConfig().getInt("maxTargetDistanceForBlock", 3);
    private final int GLOWING_EFFECT_IN_SECONDS = plugin.getConfig().getInt("glowingEffectDurationForEvidenceInSeconds", 5);
    private final CoreProtectAPI coreProtectAPI = new CoreProtectHook().getCoreProtectAPI();

    @EventHandler
    public void onBrushUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!isBrushInHand(player)) return;
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
                    if (isBrushInHand(player, true)) {
                        ItemStack brush = player.getInventory().getItemInMainHand();
                        ItemMeta meta = brush.getItemMeta();
                        if (meta != null) {
                            long[] locArr = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "block_location"), PersistentDataType.LONG_ARRAY);
                            if (locArr != null) {
                                Location loc = new Location(currentTarget.getWorld(), locArr[0], locArr[1], locArr[2]);

                                if (loc.equals(currentTarget.getLocation())) {
                                    TraceBrushUtils.setBlockGlowing(currentTarget.getWorld(), currentTarget.getLocation(), GLOWING_EFFECT_IN_SECONDS);
                                }

                            }
                        }
                    } else {
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

                    }
                    player.setCooldown(TraceBrushItem.getBlankBrushItem().getType(), 20 * 5);
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
