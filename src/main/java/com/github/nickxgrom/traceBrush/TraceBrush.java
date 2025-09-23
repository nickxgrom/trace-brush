package com.github.nickxgrom.traceBrush;

import com.github.nickxgrom.traceBrush.listeners.OnFingerprintBrushCraft;
import com.github.nickxgrom.traceBrush.listeners.UseTraceBrushOnBlock;
import com.github.nickxgrom.traceBrush.listeners.UseTraceBrushOnPlayer;
import com.github.nickxgrom.traceBrush.models.TraceBrushItem;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class TraceBrush extends JavaPlugin implements Listener {
    public Map<UUID, UUID> activePlayerTraces = new HashMap<>();
    public Map<UUID, Location> activeBlockTraces = new HashMap<>();
    public Map<UUID, Long> playersHoldingRightClickTimestamp = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(new UseTraceBrushOnPlayer(), this);
        Bukkit.getPluginManager().registerEvents(new UseTraceBrushOnBlock(), this);
        Bukkit.getPluginManager().registerEvents(new OnFingerprintBrushCraft(), this);
        TraceBrushItem.RegisterBrushItem(getConfig().getStringList("traceBrushRecipe"));

        Scoreboard mainScoreboard = this.getServer().getScoreboardManager().getMainScoreboard();
        Team traceBrushTeam = mainScoreboard.getTeam("traceBrush_team");
        if (traceBrushTeam == null) {
            traceBrushTeam = mainScoreboard.registerNewTeam("traceBrush_team");
        }
        NamedTextColor teamColor = NamedTextColor.NAMES.value(getConfig().getString("glowingEffectColor", "white").toLowerCase());
        traceBrushTeam.color(teamColor);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

}
