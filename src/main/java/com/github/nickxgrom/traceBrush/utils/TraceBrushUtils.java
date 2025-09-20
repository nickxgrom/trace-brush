package com.github.nickxgrom.traceBrush.utils;

import com.github.nickxgrom.traceBrush.TraceBrush;
import com.github.nickxgrom.traceBrush.models.TraceBrushItem;
import com.github.nickxgrom.traceBrush.models.WrittenTraceBrushItem;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class TraceBrushUtils {
    private static final NamespacedKey key = new NamespacedKey(JavaPlugin.getPlugin(TraceBrush.class), "is_trace_brush");

    public static boolean isBrushInHand(@NotNull Player player) {
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        ItemMeta meta = itemInHand.getItemMeta();

        if (meta == null) return false;

        return (
                itemInHand.getType().equals(TraceBrushItem.getTraceBrush().getType())
                || itemInHand.getType().equals(WrittenTraceBrushItem.writtenTraceBrush.getType())
        ) && Boolean.TRUE.equals(meta.getPersistentDataContainer().get(key, PersistentDataType.BOOLEAN));
    }

    public static int secondsToTicks(int seconds) {
        return seconds * 20;
    }

    public static boolean isBrushHasFingerprint(Player player) {
        if (!isBrushInHand(player)) return false;

        NamespacedKey fingerprintKey = new NamespacedKey(JavaPlugin.getPlugin(TraceBrush.class), "has_fingerprint");
        return Boolean.TRUE.equals(player.getInventory().getItemInMainHand().getItemMeta().getPersistentDataContainer().get(fingerprintKey, PersistentDataType.BOOLEAN));
    }
}
