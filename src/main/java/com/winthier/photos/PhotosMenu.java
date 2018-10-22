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
import org.bukkit.inventory.meta.MapMeta;

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
        this.photos = this.plugin.findPhotos(player.getUniqueId());
        // Create the inventory
        int photoRows = (this.photos.size() - 1) / 9 + 1;
        this.inventory = this.plugin.getServer().createInventory(this, photoRows * 9 + 9, ChatColor.DARK_PURPLE + "Photos Menu");
        for (int i = 0; i < this.photos.size(); i += 1) {
            Photo photo = this.photos.get(i);
            ItemStack item = this.plugin.createPhotoItem(photo);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + photo.getName());
            String[] lore = {
                "" + ChatColor.LIGHT_PURPLE + "Copy for " + ChatColor.WHITE + GenericEvents.formatMoney(plugin.getCopyPrice()),
                "" + ChatColor.LIGHT_PURPLE + ChatColor.ITALIC + "CLICK " + ChatColor.WHITE + "information",
                "" + ChatColor.LIGHT_PURPLE + ChatColor.ITALIC + "SHIFT+CLICK " + ChatColor.WHITE + "buy a copy"
            };
            meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
            this.inventory.addItem(item);
        }
        ItemStack item = new ItemStack(Material.MAP);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "New Photo for " + ChatColor.WHITE + GenericEvents.formatMoney(plugin.getPhotoPrice()));
        String[] lore = {
            "" + ChatColor.LIGHT_PURPLE + ChatColor.ITALIC + "CLICK " + ChatColor.WHITE + "information.",
            "" + ChatColor.LIGHT_PURPLE + ChatColor.ITALIC + "SHIFT+CLICK " + ChatColor.WHITE + "buy a new Photo."
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
        Player player = (Player)event.getWhoClicked();
        if (item == null || item.getType() == Material.AIR) return;
        if (item.getType() == Material.FILLED_MAP) {
            // Player clicked a Photo
            Photo photo = this.plugin.findPhoto(item);
            double price = this.plugin.getCopyPrice();
            if (photo == null || !player.getUniqueId().equals(photo.getOwner())) return;
            if (event.isLeftClick() && event.isShiftClick()) {
                    // Purchase
                    if (!GenericEvents.takePlayerMoney(player.getUniqueId(), price, this.plugin, "Copy Photo")) {
                        player.sendMessage(ChatColor.RED + "You can't pay " + GenericEvents.formatMoney(price) + "!");
                        return;
                    }
                    player.sendMessage(ChatColor.LIGHT_PURPLE + "Made one copy of " + ChatColor.LIGHT_PURPLE + ChatColor.ITALIC + photo.getName() + ChatColor.WHITE + " for " + ChatColor.LIGHT_PURPLE + GenericEvents.formatMoney(price) + ChatColor.WHITE + ".");
                    for (ItemStack drop: player.getInventory().addItem(plugin.createPhotoItem(photo)).values()) {
                        player.getWorld().dropItem(player.getEyeLocation(), drop).setPickupDelay(0);
                        player.sendMessage("" + ChatColor.RED + ChatColor.BOLD + "Careful!" + ChatColor.RED + " Your inventory is full. The copy was dropped.");
                    }
                    player.playSound(player.getEyeLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, SoundCategory.MASTER, 0.5f, 0.75f);
                    return;
            } else if (event.isLeftClick()) {
                player.sendMessage(ChatColor.LIGHT_PURPLE + "SHIFT+CLICK" + ChatColor.WHITE + " to make a copy of " + ChatColor.GRAY + ChatColor.ITALIC + photo.getName() + ChatColor.WHITE + " for " + ChatColor.LIGHT_PURPLE + GenericEvents.formatMoney(price) + ChatColor.WHITE + ".");
            } else if (event.isRightClick()) {
                // Maybe a detailed menu?
                return;
            }
        } else if (item.getType() == Material.MAP) {
            // Player clicked the "Buy New Photo" icon.
            double price = this.plugin.getPhotoPrice();
            if (event.isLeftClick() && event.isShiftClick()) {
                if (!GenericEvents.takePlayerMoney(player.getUniqueId(), price, this.plugin, "Buy New Photo")) {
                    player.sendMessage(ChatColor.RED + "You can't pay " + GenericEvents.formatMoney(price) + "!");
                    return;
                }
                player.sendMessage(ChatColor.LIGHT_PURPLE + "Purchased a new Photo for " + ChatColor.LIGHT_PURPLE + GenericEvents.formatMoney(price) + ChatColor.WHITE + ".");
                Photo photo = this.plugin.createPhoto(player.getUniqueId(), "Photo " + (photos.size() + 1), PhotoColor.random().color.asRGB());
                if (photo == null) {
                    this.plugin.getLogger().warning("Could not create Photo for " + player.getName() + "!");
                    player.sendMessage(ChatColor.DARK_RED + "Something went wrong during Photo creation. Please contact an administrator.");
                    GenericEvents.givePlayerMoney(player.getUniqueId(), price, this.plugin, "Photo Refund");
                    return;
                }
                for (ItemStack drop: player.getInventory().addItem(plugin.createPhotoItem(photo)).values()) {
                    player.getWorld().dropItem(player.getEyeLocation(), drop).setPickupDelay(0);
                    player.sendMessage("" + ChatColor.RED + ChatColor.BOLD + "Careful!" + ChatColor.RED + " Your inventory is full. The Photo was dropped.");
                }
                this.plugin.getPhotoCommand().suggestPhotoCommands(player, photo);
                Bukkit.getScheduler().runTask(plugin, () -> new PhotosMenu(plugin).open(player));
                player.playSound(player.getEyeLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, SoundCategory.MASTER, 0.5f, 0.75f);
            } else if (event.isLeftClick()) {
                player.sendMessage(ChatColor.LIGHT_PURPLE + "SHIFT+CLICK" + ChatColor.WHITE + " to buy a new Photo for " + ChatColor.LIGHT_PURPLE + GenericEvents.formatMoney(price) + ChatColor.WHITE + ".");
            }
        }
    }

    void onInventoryDrag(InventoryDragEvent event) {
        event.setCancelled(true);
    }
}
