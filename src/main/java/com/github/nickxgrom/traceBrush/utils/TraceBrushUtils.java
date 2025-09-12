package com.github.nickxgrom.traceBrush.utils;

import com.github.nickxgrom.traceBrush.models.TraceBrushItem;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TraceBrushUtils {
    public static boolean isBrushInHand(@NotNull Player player) {
        return player.getInventory().getItemInMainHand().getType() == TraceBrushItem.getTraceBrush().getType();
    }

    public static int secondsToTicks(int seconds) {
        return seconds * 20;
    }
}
