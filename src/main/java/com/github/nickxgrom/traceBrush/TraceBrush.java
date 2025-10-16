package com.github.nickxgrom.traceBrush;

import com.github.nickxgrom.traceBrush.listeners.OnFingerprintBrushCraft;
import com.github.nickxgrom.traceBrush.listeners.OnSuspiciousBlockBrush;
import com.github.nickxgrom.traceBrush.listeners.UseTraceBrushOnBlock;
import com.github.nickxgrom.traceBrush.listeners.UseTraceBrushOnPlayer;
import com.github.nickxgrom.traceBrush.models.TraceBrushItem;
import com.github.nickxgrom.traceBrush.utils.TraceBrushUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Shulker;
import org.bukkit.event.Listener;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class TraceBrush extends JavaPlugin implements Listener {
    public final String targetTeamName = "traceBrush_team";
    public final String evidenceTeamName = "traceBrush_evidenceTeam";
    public Map<UUID, UUID> activePlayerTraces = new HashMap<>();
    public Map<UUID, Location> activeBlockTraces = new HashMap<>();
    public Map<UUID, Long> playersHoldingRightClickTimestamp = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(new UseTraceBrushOnPlayer(), this);
        Bukkit.getPluginManager().registerEvents(new UseTraceBrushOnBlock(), this);
        Bukkit.getPluginManager().registerEvents(new OnFingerprintBrushCraft(), this);
        Bukkit.getPluginManager().registerEvents(new OnSuspiciousBlockBrush(), this);
        TraceBrushItem.RegisterBrushItem(getConfig().getStringList("traceBrushRecipe"));

        TraceBrushUtils.registerTeam(targetTeamName, getConfig().getString("playerGlowingEffectColor", "white"));
        TraceBrushUtils.registerTeam(evidenceTeamName, getConfig().getString("evidenceGlowingEffectColor", "white"));
    }

    @Override
    public void onDisable() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntitiesByClass(BlockDisplay.class)) {
                if (Boolean.TRUE.equals(entity.getPersistentDataContainer().get(new NamespacedKey(this, "is_evidence_display"), PersistentDataType.BOOLEAN))) {
                    entity.remove();
                }
            }

            Scoreboard main = this.getServer().getScoreboardManager().getMainScoreboard();
            Team evidenceTeam = main.getTeam(evidenceTeamName);
            if (evidenceTeam != null) {
                for (String entry : evidenceTeam.getEntries()) {
                    UUID uuid = UUID.fromString(entry);
                    Entity entity = Bukkit.getEntity(uuid);
                    if (entity instanceof Shulker shulker) {
                        shulker.remove();
                    }
                }
            }
        }

        Objects.requireNonNull(this.getServer().getScoreboardManager().getMainScoreboard().getTeam(targetTeamName)).unregister();
        Objects.requireNonNull(this.getServer().getScoreboardManager().getMainScoreboard().getTeam(evidenceTeamName)).unregister();
    }

}
