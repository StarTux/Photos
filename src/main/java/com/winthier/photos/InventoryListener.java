package com.winthier.photos;

import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Listen for all inventory related events, check if the involved
 * inventory belongs to a PhotosMenu, and pass the event to it.
 */
@RequiredArgsConstructor
final class InventoryListener implements Listener {
    private final PhotosPlugin plugin;

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getInventory().getHolder() instanceof PhotosMenu) {
            ((PhotosMenu)event.getInventory().getHolder()).onInventoryOpen(event);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof PhotosMenu) {
            ((PhotosMenu)event.getInventory().getHolder()).onInventoryClose(event);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof PhotosMenu) {
            ((PhotosMenu)event.getInventory().getHolder()).onInventoryClick(event);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof PhotosMenu) {
            ((PhotosMenu)event.getInventory().getHolder()).onInventoryDrag(event);
        }
    }

    /**
     * When a player right clicks a Photo in his main hand, give some
     * command suggestions.
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        switch (event.getAction()) {
        case RIGHT_CLICK_BLOCK: case RIGHT_CLICK_AIR: break;
        default: return;
        }
        switch (event.getHand()) {
        case HAND: break;
        default: return;
        }
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() != Material.FILLED_MAP) return;
        Photo photo = plugin.findPhoto(item);
        if (photo == null) return;
        plugin.updatePhotoItem(item, photo);
        plugin.getPhotoCommand().suggestPhotoCommands(player, photo);
    }

    /**
     * Deny map duplication of Photos.
     */
    @EventHandler(ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent event) {
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() != Material.FILLED_MAP) return;
        Photo photo = plugin.findPhoto(item);
        if (photo == null) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player)event.getWhoClicked();
        player.sendMessage(ChatColor.RED + "You cannot duplicate Photos. Buy a copy instead.");
    }
}
