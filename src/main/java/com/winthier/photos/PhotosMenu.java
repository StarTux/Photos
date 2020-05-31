package com.winthier.photos;

import com.winthier.generic_events.GenericEvents;
import java.util.Arrays;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
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
    boolean paged = false;
    int pageIndex = 0;
    final int photoRows = 5;
    final int pageSize = 5 * 9;
    int pageCount = 1;
    static final int PREV = 5 * 9;
    static final int NEXT = 5 * 9 + 8;

    /**
     * Open the menu for a player.  This will create an inventory
     * fitting for the given player's Photo collection, and populate
     * it with clickable items.
     */
    InventoryView open(final Player player) {
        String title = ChatColor.DARK_PURPLE + "Photos Menu";
        inventory = plugin.getServer().createInventory(this, 6 * 9, title);
        makeView(player);
        return player.openInventory(inventory);
    }

    void makeView(Player player) {
        photos = plugin.findPhotos(player.getUniqueId());
        pageCount = (photos.size() - 1) / pageSize + 1;
        paged = pageCount > 1;
        inventory.clear();
        int listOffset = pageSize * pageIndex;
        for (int i = 0; i < pageSize; i += 1) {
            final int invIndex = i;
            final int listIndex = listOffset + i;
            if (listIndex >= photos.size()) break;
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
            inventory.setItem(invIndex, item);
        }
        // Buy new photo
        ItemStack item = new ItemStack(Material.MAP);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GRAY + "Page "
                            + ChatColor.WHITE + (pageIndex + 1)
                            + ChatColor.GRAY + "/"
                            + ChatColor.WHITE + pageCount);
        String[] lore = {
            "" + ChatColor.LIGHT_PURPLE + "New Photo for " + ChatColor.WHITE
            + GenericEvents.formatMoney(plugin.getPhotoPrice()),
            "" + ChatColor.LIGHT_PURPLE + ChatColor.ITALIC + "CLICK "
            + ChatColor.WHITE + "information.",
            "" + ChatColor.LIGHT_PURPLE + ChatColor.ITALIC + "SHIFT+CLICK "
            + ChatColor.WHITE + "buy a new Photo."
        };
        meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        inventory.setItem(photoRows * 9 + 4, item);
        // Page controls
        if (paged) {
            if (pageIndex > 0) {
                ItemStack prevItem = new ItemStack(Material.ARROW);
                meta = item.getItemMeta();
                meta.setDisplayName(ChatColor.GRAY + "Page " + pageIndex);
                prevItem.setItemMeta(meta);
                inventory.setItem(PREV, prevItem);
            }
            if (pageIndex < pageCount - 1) {
                ItemStack nextItem = new ItemStack(Material.ARROW);
                meta = item.getItemMeta();
                meta.setDisplayName(ChatColor.GRAY + "Page " + (pageIndex + 2));
                nextItem.setItemMeta(meta);
                inventory.setItem(NEXT, nextItem);
            }
        }
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
        } else if (paged && event.getSlot() == PREV && pageIndex > 0) {
            pageIndex -= 1;
            makeView(player);
        } else if (paged && event.getSlot() == NEXT && pageIndex < pageCount - 1) {
            pageIndex += 1;
            makeView(player);
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
        makeView(player);
        player.playSound(player.getEyeLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER,
                         SoundCategory.MASTER, 0.5f, 0.75f);
    }

    void onInventoryDrag(InventoryDragEvent event) {
        event.setCancelled(true);
    }
}
