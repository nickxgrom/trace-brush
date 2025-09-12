package com.github.nickxgrom.traceBrush.models;

import com.github.nickxgrom.traceBrush.TraceBrush;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Objects;


public class TraceBrushItem {
    private static final ItemStack traceBrushItem = ItemStack.of(Material.RECOVERY_COMPASS);
    private static final TraceBrush plugin = JavaPlugin.getPlugin(TraceBrush.class);
    private static final NamespacedKey key = new NamespacedKey(plugin, "traceBrush");

    public static void RegisterBrushItem(List<String> recipeLines) {

        ItemMeta meta = traceBrushItem.getItemMeta();

//        NOTE: think about rework when https://docs.papermc.io/paper/dev/data-component-api/ is not experimental
        meta.displayName(Component.text("Trace Brush").color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
        meta.setItemModel(NamespacedKey.minecraft("brush"));
        meta.setEnchantmentGlintOverride(true);
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "is_trace_brush"), PersistentDataType.BOOLEAN, true);
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
}
