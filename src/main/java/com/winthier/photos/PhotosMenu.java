package com.winthier.photos;

import com.winthier.generic_events.GenericEvents;
import java.util.Arrays;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * An instance of this class contains all information for an opened
 * PhotosMenu, which is a chest GUI.  This class implements
 * InventoryHolder so a reference can be stored in the actual
 * inventory and identified by the PhotosPlugin or InventoryListener.
 *
 * To open a PhotosMenu, call `new PhotosMenu(Plugin).open(Player)`.
 */
@Getter
@RequiredArgsConstructor
final class PhotosMenu implements InventoryHolder {
    private final PhotosPlugin plugin;
    private Inventory inventory;
    private List<Photo> photos;

    /**
     * Open the menu for a player.  This will create an inventory
     * fitting for the given player's Photo collection, and populate
     * it with clickable items.
     */
    InventoryView open(Player player) {
        photos = plugin.findPhotos(player.getUniqueId());
        // Create the inventory
        int photoRows = Math.min(5, (photos.size() - 1) / 9 + 1);
        inventory = plugin.getServer().createInventory(this, photoRows * 9 + 9,
                                                       ChatColor.DARK_PURPLE + "Photos Menu");
        for (int i = 0; i < photos.size(); i += 1) {
            if (i >= photoRows * 9) break;
            Photo photo = photos.get(i);
            ItemStack item = plugin.createPhotoItem(photo);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + photo.getName());
            String[] lore = {
                "" + ChatColor.LIGHT_PURPLE + "Copy for "
                + ChatColor.WHITE + GenericEvents.formatMoney(plugin.getCopyPrice()),
                "" + ChatColor.LIGHT_PURPLE + ChatColor.ITALIC + "CLICK "
                + ChatColor.WHITE + "information",
                "" + ChatColor.LIGHT_PURPLE + ChatColor.ITALIC + "SHIFT+CLICK "
                + ChatColor.WHITE + "buy a copy"
            };
            meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
            inventory.addItem(item);
        }
        ItemStack item = new ItemStack(Material.MAP);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "New Photo for " + ChatColor.WHITE
                            + GenericEvents.formatMoney(plugin.getPhotoPrice()));
        String[] lore = {
            "" + ChatColor.LIGHT_PURPLE + ChatColor.ITALIC + "CLICK "
            + ChatColor.WHITE + "information.",
            "" + ChatColor.LIGHT_PURPLE + ChatColor.ITALIC + "SHIFT+CLICK "
            + ChatColor.WHITE + "buy a new Photo."
        };
        meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        inventory.setItem(photoRows * 9 + 4, item);
        return player.openInventory(inventory);
    }

    void onInventoryOpen(InventoryOpenEvent event) {
    }

    void onInventoryClose(InventoryCloseEvent event) {
        inventory.clear();
        inventory = null;
        photos = null;
    }

    /**
     * Copies of Photos are made via shift and left click.
     */
    void onInventoryClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getClickedInventory() == null) return;
        if (!event.getClickedInventory().equals(event.getInventory())) return;
        ItemStack item = event.getCurrentItem();
        Player player = (Player) event.getWhoClicked();
        if (item == null || item.getType() == Material.AIR) return;
        if (item.getType() == Material.FILLED_MAP) {
            // Player clicked a Photo
            Photo photo = plugin.findPhoto(item);
            double price = plugin.getCopyPrice();
            if (photo == null || !player.getUniqueId().equals(photo.getOwner())) return;
            if (event.isLeftClick() && event.isShiftClick()) {
                buyCopy(player, photo, price);
            } else if (event.isLeftClick()) {
                buyCopyInfo(player, photo, price);
            } else if (event.isRightClick()) {
                // Maybe a detailed menu?
                return;
            }
        } else if (item.getType() == Material.MAP) {
            // Player clicked the "Buy New Photo" icon.
            double price = plugin.getPhotoPrice();
            if (event.isLeftClick() && event.isShiftClick()) {
                buyPhoto(player, price);
            } else if (event.isLeftClick()) {
                player.sendMessage(ChatColor.LIGHT_PURPLE + "SHIFT+CLICK" + ChatColor.WHITE
                                   + " to buy a new Photo for " + ChatColor.LIGHT_PURPLE
                                   + GenericEvents.formatMoney(price) + ChatColor.WHITE + ".");
            }
        }
    }

    void buyCopy(Player player, Photo photo, double price) {
        // Purchase
        if (!GenericEvents.takePlayerMoney(player.getUniqueId(), price, plugin, "Copy Photo")) {
            player.sendMessage(ChatColor.RED + "You can't pay "
                               + GenericEvents.formatMoney(price) + "!");
            return;
        }
        player.sendMessage(ChatColor.LIGHT_PURPLE + "Made one copy of "
                           + ChatColor.LIGHT_PURPLE + ChatColor.ITALIC + photo.getName()
                           + ChatColor.WHITE + " for " + ChatColor.LIGHT_PURPLE
                           + GenericEvents.formatMoney(price) + ChatColor.WHITE + ".");
        ItemStack item = plugin.createPhotoItem(photo);
        for (ItemStack drop: player.getInventory().addItem(item).values()) {
            player.getWorld().dropItem(player.getEyeLocation(), drop).setPickupDelay(0);
            player.sendMessage("" + ChatColor.RED + ChatColor.BOLD + "Careful!"
                               + ChatColor.RED + " Your inventory is full. The copy was dropped.");
        }
        player.playSound(player.getEyeLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER,
                         SoundCategory.MASTER, 0.5f, 0.75f);
    }

    void buyCopyInfo(Player player, Photo photo, double price) {
        player.sendMessage(ChatColor.LIGHT_PURPLE + "SHIFT+CLICK" + ChatColor.WHITE
                           + " to make a copy of " + ChatColor.GRAY + ChatColor.ITALIC
                           + photo.getName() + ChatColor.WHITE + " for " + ChatColor.LIGHT_PURPLE
                           + GenericEvents.formatMoney(price) + ChatColor.WHITE + ".");
    }

    void buyPhoto(Player player, double price) {
        if (!GenericEvents.takePlayerMoney(player.getUniqueId(), price, plugin, "Buy New Photo")) {
            player.sendMessage(ChatColor.RED + "You can't have "
                               + GenericEvents.formatMoney(price) + "!");
            return;
        }
        player.sendMessage(ChatColor.LIGHT_PURPLE + "Purchased a new Photo for "
                           + ChatColor.LIGHT_PURPLE + GenericEvents.formatMoney(price)
                           + ChatColor.WHITE + ".");
        Photo photo = plugin.createPhoto(player.getUniqueId(), "Photo " + (photos.size() + 1),
                                         PhotoColor.random().color.asRGB());
        if (photo == null) {
            plugin.getLogger().warning("Could not create Photo for " + player.getName() + "!");
            player.sendMessage(ChatColor.DARK_RED + "Something went wrong during Photo creation."
                               + " Please contact an administrator.");
            GenericEvents.givePlayerMoney(player.getUniqueId(), price, plugin, "Photo Refund");
            return;
        }
        ItemStack item = plugin.createPhotoItem(photo);
        for (ItemStack drop: player.getInventory().addItem(item).values()) {
            player.getWorld().dropItem(player.getEyeLocation(), drop).setPickupDelay(0);
            player.sendMessage("" + ChatColor.RED + ChatColor.BOLD + "Careful!" + ChatColor.RED
                               + " Your inventory is full. The Photo was dropped.");
        }
        plugin.getPhotoCommand().suggestPhotoCommands(player, photo);
        Bukkit.getScheduler().runTask(plugin, () -> new PhotosMenu(plugin).open(player));
        player.playSound(player.getEyeLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER,
                         SoundCategory.MASTER, 0.5f, 0.75f);
    }

    void onInventoryDrag(InventoryDragEvent event) {
        event.setCancelled(true);
    }
}
