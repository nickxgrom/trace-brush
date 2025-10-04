package com.github.nickxgrom.traceBrush.utils;

import com.github.nickxgrom.traceBrush.TraceBrush;
import com.github.nickxgrom.traceBrush.models.TraceBrushItem;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class TraceBrushUtils {
    private static final TraceBrush plugin = JavaPlugin.getPlugin(TraceBrush.class);
    private static final NamespacedKey key = new NamespacedKey(plugin, "is_trace_brush");
    private static final String EVIDENCE_GLOWING_COLOR = plugin.getConfig().getString("evidenceGlowingEffectColor", "white");

    private static Color getColor(String colorName) {
        return switch (colorName) {
            case "white" -> Color.WHITE;
            case "silver" -> Color.SILVER;
            case "gray", "grey" -> Color.GRAY;
            case "black" -> Color.BLACK;
            case "red" -> Color.RED;
            case "maroon" -> Color.MAROON;
            case "yellow" -> Color.YELLOW;
            case "olive" -> Color.OLIVE;
            case "lime" -> Color.LIME;
            case "green" -> Color.GREEN;
            case "aqua" -> Color.AQUA;
            case "teal" -> Color.TEAL;
            case "blue" -> Color.BLUE;
            case "navy" -> Color.NAVY;
            case "fuchsia" -> Color.FUCHSIA;
            case "purple" -> Color.PURPLE;
            case "orange" -> Color.ORANGE;
            default -> Color.WHITE;
        };
    }

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

    public static void setBlockGlowing(Block block, int durationInSeconds) {
        BlockDisplay display = block.getWorld().spawn(block.getLocation(), BlockDisplay.class);
        display.setBlock(block.getType().createBlockData());
        display.setGlowing(true);
        display.setInvisible(true);
        display.setInvulnerable(true);
        display.setNoPhysics(true);
        if (EVIDENCE_GLOWING_COLOR != null) {
            display.setGlowColorOverride(getColor(EVIDENCE_GLOWING_COLOR));
        }

        float scaleDelta = .001f;
        float vectorValue = 1 + scaleDelta;

        Vector3f newScale = new Vector3f(vectorValue, vectorValue, vectorValue);

        float shift = -(scaleDelta / 2);
        Vector3f translation = new Vector3f(shift, shift, shift);


        display.setTransformation(new Transformation(
                translation,
                new Quaternionf(),
                newScale,
                new Quaternionf()
        ));
//        TODO: problem with doors
        display.setBlock(block.getBlockData());
        display.setBrightness(new Display.Brightness(15, 15));

//        might be laggy, check
        new BukkitRunnable() {
            final int maxTicks = secondsToTicks(durationInSeconds);
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= maxTicks || block.getWorld().getBlockAt(block.getLocation()).getType() == Material.AIR) {
                    display.remove();
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
