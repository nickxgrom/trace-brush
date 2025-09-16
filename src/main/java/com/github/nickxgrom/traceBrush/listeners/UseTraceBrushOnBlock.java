package com.github.nickxgrom.traceBrush.listeners;

import com.github.nickxgrom.traceBrush.CoreProtectHook;
import com.github.nickxgrom.traceBrush.TraceBrush;
import com.github.nickxgrom.traceBrush.models.TraceBrushItem;
import com.github.nickxgrom.traceBrush.models.types.CoreProtectInteractionType;
import com.github.nickxgrom.traceBrush.utils.TraceBrushUtils;
import net.coreprotect.CoreProtectAPI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Openable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static com.github.nickxgrom.traceBrush.utils.TraceBrushUtils.isBrushInHand;
import static com.github.nickxgrom.traceBrush.utils.TraceBrushUtils.secondsToTicks;

public class UseTraceBrushOnBlock implements Listener {
    private static final Set<Material> SPECIAL_GUI_BLOCKS = Set.of(
            Material.CRAFTING_TABLE,
            Material.ANVIL,
            Material.ENCHANTING_TABLE,
            Material.SMITHING_TABLE,
            Material.GRINDSTONE,
            Material.CARTOGRAPHY_TABLE,
            Material.STONECUTTER,
            Material.LOOM,
            Material.ENDER_CHEST
    );
    private final TraceBrush plugin = JavaPlugin.getPlugin(TraceBrush.class);
    private final int RUB_TIME_IN_SECONDS = plugin.getConfig().getInt("rubTimeInSeconds");
    private final int MAX_TARGET_DISTANCE = plugin.getConfig().getInt("maxTargetDistance");
    private final CoreProtectAPI coreProtectAPI = new CoreProtectHook().getCoreProtectAPI();

    @EventHandler
    public void onBrushUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!isBrushInHand(player)) return;
        if (event.getClickedBlock() == null) return;

        UUID playerId = player.getUniqueId();
        Block targetBlock = event.getClickedBlock();

        if (targetBlock.getType() == Material.SUSPICIOUS_SAND ||
                targetBlock.getType() == Material.SUSPICIOUS_GRAVEL) {
            event.setCancelled(true);
        }

        if (isInteractiveBlock(targetBlock)) {
            return;
        }

        if (TraceBrushUtils.isBrushHasFingerprint(player)) {
            event.setCancelled(true);
        }

        if (plugin.activeBlockTraces.containsKey(playerId)) return;
        plugin.activeBlockTraces.put(playerId, targetBlock.getLocation());

        new BukkitRunnable() {
            final int maxTicks = secondsToTicks(RUB_TIME_IN_SECONDS);
            int ticks = 0;

            @Override
            public void run() {
                ticks++;

                boolean isPlayerInProgress = plugin.activeBlockTraces.containsKey(playerId);
                boolean isLookingAtTarget;

                try {
//                    problem: if player look at block and move mouse to another block -> nothing happens
                    isLookingAtTarget = Objects.requireNonNull(player.getTargetBlockExact(MAX_TARGET_DISTANCE)).getLocation().equals(targetBlock.getLocation());
                } catch (Exception e) {
                    isLookingAtTarget = false;
                }

                if (!isPlayerInProgress || !player.isHandRaised() || !isLookingAtTarget || !isBrushInHand(player)) {
                    cleanup();
                    return;
                }

                if (ticks >= maxTicks) {
                    List<String[]> lookup = coreProtectAPI.blockLookup(targetBlock, 0);

                    if (!lookup.isEmpty()) {
                        String placedBy = null;
                        for (String[] row : lookup) {
                            CoreProtectAPI.ParseResult result = coreProtectAPI.parseResult(row);

                            if (result.getActionId() == CoreProtectInteractionType.PLACEMENT.ordinal()) {
                                placedBy = result.getPlayer();
                                break;
                            }
                        }

                        if (placedBy == null) {
                            TraceBrushItem.writeFingerprintToBrush(player, null, null);
                            return;
                        }

                        TraceBrushItem.writeFingerprintToBrush(player, Bukkit.getOfflinePlayer(placedBy).getUniqueId(), targetBlock);
                    } else {
                        TraceBrushItem.writeFingerprintToBrush(player, null, null);
                    }

                    cleanup();
                }
            }

            private void cleanup() {
                plugin.activeBlockTraces.remove(playerId);
                cancel();
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private boolean isInteractiveBlock(Block block) {
        Material type = block.getType();

        boolean isOpenable = block.getBlockData() instanceof Openable;
        boolean isInventory = block.getState() instanceof InventoryHolder;
        boolean isSpecialGui = SPECIAL_GUI_BLOCKS.contains(type) || type.name().endsWith("_BED");

        return isOpenable || isInventory || isSpecialGui;
    }
}
