package com.github.nickxgrom.traceBrush.utils;

import com.github.nickxgrom.traceBrush.TraceBrush;
import com.github.nickxgrom.traceBrush.models.TraceBrushItem;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class TraceBrushUtils {
    private static final NamespacedKey key = new NamespacedKey(JavaPlugin.getPlugin(TraceBrush.class), "is_trace_brush");

    public static boolean isBrushInHand(@NotNull Player player, boolean hasFingerprint) {
        ItemStack item = player.getInventory().getItemInMainHand();
        ItemMeta meta = item.getItemMeta();

        if (meta == null) return false;

        String type = (hasFingerprint ? TraceBrushItem.getWrittenBrushItem() : TraceBrushItem.getBlankBrushItem()).getType().toString();

        return item.getType().toString().equals(type)
                && Boolean.TRUE.equals(meta.getPersistentDataContainer().get(key, PersistentDataType.BOOLEAN));
    }

    public static int secondsToTicks(int seconds) {
        return seconds * 20;
    }
}
