package com.github.nickxgrom.traceBrush;

import com.github.nickxgrom.traceBrush.listeners.UseTraceBrush;
import com.github.nickxgrom.traceBrush.models.TraceBrushItem;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class TraceBrush extends JavaPlugin implements Listener {
    public Map<UUID, UUID> activeTraces = new HashMap<>();
    public Map<UUID, Long> playersHoldingRightClickTimestamp = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(new UseTraceBrush(), this);
        TraceBrushItem.RegisterBrushItem();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

}
