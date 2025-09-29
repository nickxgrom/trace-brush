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
import org.bukkit.Sound;
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
    private final int BRUSH_COOLDOWN_IN_SECONDS = plugin.getConfig().getInt("brushCooldownInSeconds", 5);
    private final CoreProtectAPI coreProtectAPI = new CoreProtectHook().getCoreProtectAPI();

    @EventHandler
    public void onBrushUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!isBrushInHand(player)) return;
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
            Block currentTarget = targetBlock;

            @Override
            public void run() {
                // TODO: hide coordinates in brush lore if config option is set
                // TODO: replace invisible glowing shulker with BlockDisplay
                // TODO: prevent sus gravel and sand from break with fingerprint brush

                if (isBrushInHand(player, true)) {
                    boolean isPlayerInProgress = plugin.activeBlockTraces.containsKey(playerId);
                    boolean isKeyPressed = plugin.playersHoldingRightClickTimestamp.get(playerId) != null && System.currentTimeMillis() - plugin.playersHoldingRightClickTimestamp.get(playerId) <= 200;

                    if (!isPlayerInProgress || !isKeyPressed || !isBrushInHand(player) || player.hasCooldown(player.getInventory().getItemInMainHand())) {
                        cleanup();
                        return;
                    }

                    if (ticks % 10 == 0) {
                        Location soundLocation = currentTarget.getLocation().add(0.5, 1, 0.5);
                        Sound sound = Sound.ITEM_GLOW_INK_SAC_USE;
                        soundLocation.getWorld().playSound(soundLocation, sound, 1.0f, 1.0f);
                        player.swingMainHand();
                    }

                    if (ticks >= maxTicks) {
                        verifyFingerprint(player.getInventory().getItemInMainHand(), currentTarget);

                        setBrushCooldown(player);
                        cleanup();
                    }
                } else {
                    Block targetBlockExact = player.getTargetBlockExact(MAX_TARGET_DISTANCE);

                    if (targetBlockExact == null || !player.isHandRaised()) {
                        cleanup();
                        return;
                    }

                    if (!targetBlockExact.getLocation().equals(currentTarget.getLocation())) {
                        currentTarget = targetBlockExact;
                        ticks = 0;
                    }

                    if (ticks >= maxTicks) {
                        collectFingerprint(player, currentTarget);

                        setBrushCooldown(player);
                        cleanup();
                    }
                }

                ticks++;
            }

            private void cleanup() {
                plugin.activeBlockTraces.remove(playerId);
                plugin.playersHoldingRightClickTimestamp.remove(playerId);
                cancel();
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private void collectFingerprint(Player player, Block targetBlock) {
        List<String[]> lookup = coreProtectAPI.blockLookup(targetBlock, 0);

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

            TraceBrushItem.writeFingerprintToBrush(player, Bukkit.getOfflinePlayer(placedBy).getUniqueId(), targetBlock);
        } else {
            TraceBrushItem.writeFingerprintToBrush(player, null, null);
        }
    }

    private void verifyFingerprint(ItemStack brush, Block targetBlock) {
        ItemMeta meta = brush.getItemMeta();
        if (meta != null) {
            long[] locArr = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "block_location"), PersistentDataType.LONG_ARRAY);
            if (locArr != null) {
                Location loc = new Location(targetBlock.getWorld(), locArr[0], locArr[1], locArr[2]);

                if (loc.equals(targetBlock.getLocation())) {
                    TraceBrushUtils.setBlockGlowing(targetBlock.getWorld(), targetBlock.getLocation(), GLOWING_EFFECT_IN_SECONDS);
                } else {
                    // TODO: particles when traced block is not written to brush
                }
            }
        }
    }

    private void setBrushCooldown(Player player) {
        player.setCooldown(TraceBrushItem.getBlankBrushItem().getType(), secondsToTicks(BRUSH_COOLDOWN_IN_SECONDS));
        player.setCooldown(TraceBrushItem.getWrittenBrushItem().getType(), secondsToTicks(BRUSH_COOLDOWN_IN_SECONDS));
    }
}
