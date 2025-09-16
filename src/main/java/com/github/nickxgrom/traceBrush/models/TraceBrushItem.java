package com.github.nickxgrom.traceBrush.models;

import com.github.nickxgrom.traceBrush.TraceBrush;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.github.nickxgrom.traceBrush.utils.TraceBrushUtils.isBrushInHand;

public class TraceBrushItem extends ItemStack {
    private static final ItemStack traceBrushItem = ItemStack.of(Material.BRUSH);
    private static final TraceBrush plugin = JavaPlugin.getPlugin(TraceBrush.class);
    private static final NamespacedKey key = new NamespacedKey(plugin, "traceBrush");

    public static void RegisterBrushItem(List<String> recipeLines) {

        ItemMeta meta = traceBrushItem.getItemMeta();

//        NOTE: think about rework when https://docs.papermc.io/paper/dev/data-component-api/ is not experimental
        meta.displayName(Component.text("Trace Brush").color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
        meta.setItemModel(NamespacedKey.minecraft("brush"));
        meta.setEnchantmentGlintOverride(true);
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "is_trace_brush"), PersistentDataType.BOOLEAN, true);
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "unique_id"), PersistentDataType.STRING, UUID.randomUUID().toString());
        meta.setMaxStackSize(1);
        traceBrushItem.setItemMeta(meta);

        registerRecipe(recipeLines);
    }

    public static ItemStack getTraceBrush() {
        return traceBrushItem;
    }

    private static void registerRecipe(List<String> recipeLines) {
        List<Material> recipeMaterials = recipeLines.stream().map(line -> {
            try {
                return Material.valueOf(line.toUpperCase());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid material in config traceBrushCraft: " + line);
                return null;
            }
        }).filter(Objects::nonNull).toList();

        ShapedRecipe recipe = new ShapedRecipe(key, traceBrushItem);
        if (recipeMaterials.size() != 9) {
            plugin.getLogger().warning("Wrong number of materials in config traceBrushCraft. Using default recipe.");
            recipe.shape("DDD", "DBD", "DDD");
            recipe.setIngredient('D', Material.DIAMOND);
            recipe.setIngredient('B', Material.BRUSH);
        } else {
            recipe.shape("ABC", "DEF", "GHI");
            char ingredientChar = 'A';

            for (Material material : recipeMaterials) {
                recipe.setIngredient(ingredientChar, material);
                ingredientChar++;
            }
        }

        plugin.getServer().addRecipe(recipe);
    }

    public static void writeFingerprintToBrush(Player player, @Nullable UUID placedBy, @Nullable Block block) {
        ItemStack brushItem = getFingerprintBrushFromHand(player);

        if (brushItem == null) return;

        ItemMeta meta = brushItem.getItemMeta();
        if (meta == null) return;


        if (block == null) {
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "has_fingerprint"), PersistentDataType.BOOLEAN, false);
            meta.lore(List.of(
                    Component.text("No fingerprint found").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)
            ));
        } else {
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "has_fingerprint"), PersistentDataType.BOOLEAN, true);
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "placed_by"), PersistentDataType.STRING, placedBy != null ? placedBy.toString() : "");
            meta.getPersistentDataContainer().set(new NamespacedKey(JavaPlugin.getPlugin(TraceBrush.class), "timestamp"), PersistentDataType.LONG, System.currentTimeMillis());
            meta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "block_location"),
                    PersistentDataType.LONG_ARRAY, new long[]{
                            block.getLocation().getBlockX(),
                            block.getLocation().getBlockY(),
                            block.getLocation().getBlockZ(),
                    }
            );
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "block_material"), PersistentDataType.STRING, block.getType().data.getCanonicalName());
            meta.lore(List.of(
                    Component.text("Material: ")
                            .append(Component.translatable(Objects.requireNonNull(block.getType().getBlockTranslationKey())))
                            .color(NamedTextColor.WHITE)
                            .decoration(TextDecoration.ITALIC, false),
                    Component.text(String.format("%d %d %d", block.getLocation().getBlockX(), block.getLocation().getBlockY(), block.getLocation().getBlockZ())).color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)
            ));
        }

        brushItem.setItemMeta(meta);
    }

    public static ItemStack getFingerprintBrushFromHand(Player player) {
        if (isBrushInHand(player)) {
            return player.getInventory().getItemInMainHand();
        } else {
            return null;
        }
    }
}
