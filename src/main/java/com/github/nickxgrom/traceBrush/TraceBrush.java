package com.github.nickxgrom.traceBrush;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class TraceBrush extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
    }


    @EventHandler
    public void onBrushUse(PlayerInteractEntityEvent event) {
//        Add animation of brushing for 2s as default and move to config
        if (!(event.getRightClicked() instanceof Player target) ) return;

        System.out.println(target);
    }


    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
