package com.github.nickxgrom.traceBrush.models;

import com.github.nickxgrom.traceBrush.TraceBrush;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
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
    private static final TraceBrush plugin = JavaPlugin.getPlugin(TraceBrush.class);
    private static final NamespacedKey key = new NamespacedKey(plugin, "traceBrush");

    private static final ItemStack blankBrushItem = ItemStack.of(Material.BRUSH);
    private static final ItemStack writtenBrushItem = ItemStack.of(Material.RECOVERY_COMPASS);

    public static ItemStack getBlankBrushItem() {
        return blankBrushItem;
    }

    public static ItemStack getWrittenBrushItem() {
        return writtenBrushItem;
    }

    public static void RegisterBrushItem(List<String> recipeLines) {
        setBaseItemMeta(getBlankBrushItem());

        List<Material> recipeMaterials = recipeLines.stream().map(line -> {
            try {
                return Material.valueOf(line.toUpperCase());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid material in config traceBrushCraft: " + line);
                return null;
            }
        }).filter(Objects::nonNull).toList();

        ShapedRecipe recipe = new ShapedRecipe(key, getBlankBrushItem());
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

    //        TODO: think about rework when https://docs.papermc.io/paper/dev/data-component-api/ is not experimental
    private static void setBaseItemMeta(ItemStack item) {
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("Trace Brush").color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
        meta.setEnchantmentGlintOverride(true);
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "is_trace_brush"), PersistentDataType.BOOLEAN, true);
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "unique_id"), PersistentDataType.STRING, UUID.randomUUID().toString());
        meta.setMaxStackSize(1);

        item.setItemMeta(meta);
    }

    public static void writeFingerprintToBrush(Player player, @Nullable UUID placedBy, @Nullable Block block) {
        ItemStack brushItem = isBrushInHand(player, false) ? player.getInventory().getItemInMainHand() : null;

        if (brushItem == null) return;

        ItemMeta meta = brushItem.getItemMeta();
        if (meta == null) return;


        if (block == null) {
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "has_fingerprint"), PersistentDataType.BOOLEAN, false);
            meta.lore(List.of(
                    Component.text("No fingerprint found").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)
            ));
            brushItem.setItemMeta(meta);
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
//            TODO: add setting to display material as png with resource pack
            meta.lore(List.of(
                    Component.text("Material: ")
                            .append(Component.translatable(Objects.requireNonNull(block.getType().getBlockTranslationKey())))
                            .color(NamedTextColor.WHITE)
                            .decoration(TextDecoration.ITALIC, false),
//                    TODO: configure visibility of location, add particles to know which block was brushed
                    Component.text(String.format("%d %d %d", block.getLocation().getBlockX(), block.getLocation().getBlockY(), block.getLocation().getBlockZ())).color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)
            ));

            ItemStack writtenTraceBrush = new ItemStack(TraceBrushItem.getWrittenBrushItem().getType());
            meta.setItemModel(Material.BRUSH.getKey());
            writtenTraceBrush.setItemMeta(meta);
            player.getInventory().setItemInMainHand(writtenTraceBrush);

            Location soundLocation = block.getLocation().add(0.5, 0.5, 0.5);
            Sound sound = Sound.ITEM_GLOW_INK_SAC_USE;
            soundLocation.getWorld().playSound(soundLocation, sound, 1.0f, 1.0f);
        }
    }
}
