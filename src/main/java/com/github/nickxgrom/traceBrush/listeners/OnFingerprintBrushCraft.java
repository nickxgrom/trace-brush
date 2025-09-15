package com.github.nickxgrom.traceBrush.listeners;

import com.github.nickxgrom.traceBrush.TraceBrush;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public class OnFingerprintBrushCraft implements Listener {
    @EventHandler
    public void onFingerprintBrushCraft(CraftItemEvent event) {
        ItemStack item = event.getCurrentItem();
        if (item == null) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        meta.getPersistentDataContainer().set(new NamespacedKey(JavaPlugin.getPlugin(TraceBrush.class), "unique_id"), PersistentDataType.STRING, UUID.randomUUID().toString());

        item.setItemMeta(meta);
    }
}
