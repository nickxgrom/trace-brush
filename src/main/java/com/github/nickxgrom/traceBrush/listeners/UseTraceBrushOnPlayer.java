package com.github.nickxgrom.traceBrush.listeners;

import com.github.nickxgrom.traceBrush.TraceBrush;
import com.github.nickxgrom.traceBrush.models.TraceBrushItem;
import com.github.nickxgrom.traceBrush.utils.TraceBrushUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Objects;
import java.util.UUID;

import static com.github.nickxgrom.traceBrush.utils.TraceBrushUtils.isBrushInHand;
import static com.github.nickxgrom.traceBrush.utils.TraceBrushUtils.secondsToTicks;

public class UseTraceBrushOnPlayer implements Listener {
    private final TraceBrush plugin = JavaPlugin.getPlugin(TraceBrush.class);
    private final int RUB_TIME_IN_SECONDS = plugin.getConfig().getInt("rubTimeInSecondsForTarget", 5);
    private final int MAX_TARGET_DISTANCE = plugin.getConfig().getInt("maxTargetDistanceForTarget", 3);
    private final int BRUSH_COOLDOWN = plugin.getConfig().getInt("brushCooldownInSeconds", 5);
    private final int GLOWING_EFFECT_IN_SECONDS = plugin.getConfig().getInt("glowingEffectDurationInSeconds", 5);
    private final long HOLD_TIMEOUT_MS = 200;

    @EventHandler
    public void onBrushUse(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();

        if (
                isBrushInHand(player, false)
                        || player.hasCooldown(TraceBrushItem.getWrittenBrushItem())
        )
            return;
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
                boolean isPlayerInProgress = plugin.activePlayerTraces.containsKey(playerId);
                boolean isKeyPressed = plugin.playersHoldingRightClickTimestamp.get(playerId) != null && System.currentTimeMillis() - plugin.playersHoldingRightClickTimestamp.get(playerId) <= HOLD_TIMEOUT_MS;
                boolean isLookingAtTarget = player.getTargetEntity(MAX_TARGET_DISTANCE) != null && Objects.requireNonNull(player.getTargetEntity(3)).getUniqueId().equals(targetId);

                if (!isPlayerInProgress || !isKeyPressed || !isLookingAtTarget || !isBrushInHand(player, true)) {
                    cleanup();
                    return;
                }

                if (ticks % 10 == 0) {
                    Location soundLocation = target.getLocation().add(0.5, 1, 0.5);
                    Sound sound = Sound.ITEM_GLOW_INK_SAC_USE;
                    soundLocation.getWorld().playSound(soundLocation, sound, 1.0f, 1.0f);
                    player.swingMainHand();
                }

                if (ticks >= maxTicks) {
                    player.setCooldown(TraceBrushItem.getWrittenBrushItem(), TraceBrushUtils.secondsToTicks(BRUSH_COOLDOWN));

                    ItemStack brush = isBrushInHand(player, true) ? player.getInventory().getItemInMainHand() : null;
                    if (brush == null) return;

                    ItemMeta meta = brush.getItemMeta();
                    if (meta == null) return;

                    String uuidString = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "placed_by"), PersistentDataType.STRING);
                    if (uuidString == null) return;

                    UUID fingerprintPlacedByUUID = UUID.fromString(uuidString);
                    if (fingerprintPlacedByUUID.equals(targetId)) {
                        target.setGlowing(true);
                    }

                    Bukkit.getScheduler().runTaskLater(plugin, () -> target.setGlowing(false), TraceBrushUtils.secondsToTicks(GLOWING_EFFECT_IN_SECONDS));

                    cleanup();
                }

                ticks++;
            }

            private void cleanup() {
                plugin.activePlayerTraces.remove(playerId);
                plugin.playersHoldingRightClickTimestamp.remove(playerId);
                cancel();
            }
        }.runTaskTimer(plugin, 0, 1);
    }
}
