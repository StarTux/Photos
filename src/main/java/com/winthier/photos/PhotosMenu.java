package com.winthier.photos;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class PhotosMenu implements Listener {
    private final PhotosPlugin plugin;
    private Map<UUID, InventoryView> openMenus = new HashMap<UUID, InventoryView>();
    private boolean enabled;

    public PhotosMenu(PhotosPlugin plugin) {
        this.plugin = plugin;
    }

    public void onEnable() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        enabled = true;
    }

    public void onDisable() {
        for (InventoryView view : openMenus.values()) view.close();
        openMenus.clear();
    }

    @EventHandler(ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent event) {
        ItemStack item = event.getCurrentItem();
        if (item != null && item.getType() == Material.MAP) {
            short mapId = item.getDurability();
            if (plugin.photosConfig.hasMap(mapId)) {
                Player player = (Player)event.getWhoClicked();
                player.sendMessage("" + ChatColor.RED + "You can't duplicate photos");
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent event) {
        openMenus.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (openMenus.get(event.getWhoClicked().getUniqueId()) != null) {
            event.setCancelled(true);
            if (event.isShiftClick() && event.getRawSlot() < event.getView().getTopInventory().getSize()) {
                Player player = (Player)event.getWhoClicked();
                ItemStack item = event.getCurrentItem();
                if (item != null && item.getType() == Material.MAP) {
                    if (!plugin.economy.has(player, plugin.mapPrice)) {
                        player.sendMessage("" + ChatColor.RED + "You do not have enough money");
                        return;
                    }
                    if (!plugin.economy.withdrawPlayer(player, plugin.mapPrice).transactionSuccess()) {
                        player.sendMessage("" + ChatColor.RED + "Payment error");
                        return;
                    }
                    player.sendMessage("" + ChatColor.GREEN + "Purchased for " + plugin.economy.format(plugin.mapPrice));
                    short id = item.getDurability();
                    Photo photo = plugin.getPhoto(id);
                    ItemStack drop = photo.createItem(item.getItemMeta().getDisplayName());
                    player.getWorld().dropItem(player.getEyeLocation(), drop);
                    Location loc = player.getLocation();
                    plugin.getLogger().info(String.format("Player %s bought map #%d in %s at %d,%d,%d", player.getName(), id, loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
                }
                if (item != null && item.getType() == Material.EMPTY_MAP) {
                    plugin.createPhoto(player, "Photo");
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (openMenus.get(event.getWhoClicked().getUniqueId()) != null) {
            event.setCancelled(true);
        }
    }

    public void openMenu(Player player) {
        if (!enabled) return;
        List<Short> list = plugin.photosConfig.getPlayerMaps(player);
        int size = ((list.size() + 1 - 1) / 9 + 1) * 9;
        Inventory inventory = plugin.getServer().createInventory(player, size, "Photos Menu");
        for (Short id : list) {
            ItemStack item = new ItemStack(Material.MAP, 1, id);
            String name = plugin.photosConfig.getMapName(id);
            ItemMeta meta = item.getItemMeta();
            if (name != null) meta.setDisplayName("" + ChatColor.GOLD + name);
            else meta.setDisplayName("" + ChatColor.GOLD + "Map");
            meta.setLore(Arrays.asList("" + ChatColor.BLUE + "Shift click to buy",
                                       "" + ChatColor.BLUE + "a copy for " + ChatColor.WHITE + plugin.economy.format(plugin.mapPrice)));
            item.setItemMeta(meta);
            inventory.addItem(item);
        } {
            ItemStack item = new ItemStack(Material.EMPTY_MAP, 1);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("" + ChatColor.DARK_GRAY + "New Photo");
            meta.setLore(Arrays.asList("" + ChatColor.BLUE + "Shift click to create",
                                       "" + ChatColor.BLUE + "a new blank photo.",
                                       "" + ChatColor.BLUE + "Blank photos left: " + ChatColor.WHITE + plugin.photosConfig.getPlayerBlanks(player)));
            item.setItemMeta(meta);
            inventory.addItem(item);
        }
        InventoryView view = player.openInventory(inventory);
        openMenus.put(player.getUniqueId(), view);
        player.sendMessage("" + ChatColor.GREEN + "Photos Menu");
    }
}
