package com.github.nickxgrom.traceBrush.models;

import com.github.nickxgrom.traceBrush.TraceBrush;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
import io.papermc.paper.datacomponent.item.ItemLore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import javax.xml.crypto.Data;


public class TraceBrushItem {
    public static void RegisterBrushItem() {
        final TraceBrush plugin = JavaPlugin.getPlugin(TraceBrush.class);
        NamespacedKey key = new NamespacedKey(plugin, "traceBrush");
        ItemStack traceBrush = ItemStack.of(Material.RECOVERY_COMPASS);
        ItemMeta meta = traceBrush.getItemMeta();

//        NOTE: think about rework when https://docs.papermc.io/paper/dev/data-component-api/ is not experimental
        meta.displayName(Component.text("Trace Brush").color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
        meta.setItemModel(NamespacedKey.minecraft( "brush"));
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin,"is_trace_brush"), PersistentDataType.BOOLEAN, true);
        traceBrush.setItemMeta(meta);

//        TODO change recipe or move to config
        ShapedRecipe recipe = new ShapedRecipe(key, traceBrush);
        recipe.shape("DDD", "DBD", "DDD");
        recipe.setIngredient('D', Material.DIAMOND);
        recipe.setIngredient('B', Material.BRUSH);

        plugin.getServer().addRecipe(recipe);
    };
}
