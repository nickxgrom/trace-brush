package com.github.nickxgrom.traceBrush.utils;

import com.github.nickxgrom.traceBrush.TraceBrush;
import com.github.nickxgrom.traceBrush.models.TraceBrushItem;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Shulker;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;

public class TraceBrushUtils {
    private static final TraceBrush plugin = JavaPlugin.getPlugin(TraceBrush.class);
    private static final NamespacedKey key = new NamespacedKey(plugin, "is_trace_brush");

    public static boolean isBrushInHand(@NotNull Player player, boolean hasFingerprint) {
        ItemStack item = player.getInventory().getItemInMainHand();
        ItemMeta meta = item.getItemMeta();

        if (meta == null) return false;

        String type = (hasFingerprint ? TraceBrushItem.getWrittenBrushItem() : TraceBrushItem.getBlankBrushItem()).getType().toString();

        return item.getType().toString().equals(type)
                && Boolean.TRUE.equals(meta.getPersistentDataContainer().get(key, PersistentDataType.BOOLEAN));
    }

    public static boolean isBrushInHand(@NotNull Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        ItemMeta meta = item.getItemMeta();

        if (meta == null) return false;


        return (item.getType().toString().equals(TraceBrushItem.getBlankBrushItem().getType().toString())
                || item.getType().toString().equals(TraceBrushItem.getWrittenBrushItem().getType().toString())
        ) && Boolean.TRUE.equals(meta.getPersistentDataContainer().get(key, PersistentDataType.BOOLEAN));
    }


    public static int secondsToTicks(int seconds) {
        return seconds * 20;
    }

    public static void setPlayerGlowing(Player player, int durationInSeconds) {
        Scoreboard scoreboard = plugin.getServer().getScoreboardManager().getMainScoreboard();
        Team team = scoreboard.getTeam(plugin.targetTeamName);

        if (team != null) {
            team.addPlayer(player);
        }

        player.setGlowing(true);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.setGlowing(false);
            if (team != null) {
                team.removePlayer(player);
            }
        }, TraceBrushUtils.secondsToTicks(durationInSeconds));
    }

    public static void setBlockGlowing(World world, Location location, int durationInSeconds) {
        Shulker shulker = (Shulker) world.spawnEntity(location, EntityType.SHULKER);
        Scoreboard scoreboard = plugin.getServer().getScoreboardManager().getMainScoreboard();
        Team team = scoreboard.getTeam(plugin.evidenceTeamName);

        if (team != null) {
            team.addEntity(shulker);
        }

        shulker.setInvisible(true);
        shulker.setInvulnerable(true);
        shulker.setAI(false);
        shulker.setCollidable(false);
        shulker.setGlowing(true);

//        might be laggy, check
        new BukkitRunnable() {
            final int maxTicks = secondsToTicks(durationInSeconds);
            int ticks = 0;

            @Override
            public void run() {
//                BlockDisplay display = currentTarget.getWorld().spawn(currentTarget.getLocation(), BlockDisplay.class, d -> {
//                    d.setBlock(Material.ANVIL.createBlockData()); // блок, который имитируем
//                    d.setGlowing(true);
//                    d.setInvulnerable(true);
//                    d.setGlowColorOverride(Color.NAVY);
//                });

                if (ticks >= maxTicks || world.getBlockAt(location).getType() == Material.AIR) {
                    shulker.setGlowing(false);
                    if (team != null) {
                        team.removeEntity(shulker);
                    }

                    shulker.remove();
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    public static void registerTeam(String name, String color) {
        Scoreboard mainScoreboard = plugin.getServer().getScoreboardManager().getMainScoreboard();
        Team traceBrushTeam = mainScoreboard.getTeam(name);
        if (traceBrushTeam == null) {
            traceBrushTeam = mainScoreboard.registerNewTeam(name);
        }
        NamedTextColor teamColor = NamedTextColor.NAMES.value(color.toLowerCase());
        traceBrushTeam.color(teamColor);
    }
}
