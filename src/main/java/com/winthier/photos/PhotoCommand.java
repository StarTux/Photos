package com.winthier.photos;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

public class PhotoCommand implements CommandExecutor {
        public final PhotosPlugin plugin;
        private final Map<String, Long> loadCooldowns = new HashMap<String, Long>();

        public PhotoCommand(PhotosPlugin plugin) {
                this.plugin = plugin;
        }

        public void load(final Player player, final String url) {
                ItemStack item = player.getItemInHand();
                if (item == null || item.getType() != Material.MAP) {
                        player.sendMessage("" + ChatColor.RED + "You must hold a photo");
                        return;
                }
                short mapId = item.getDurability();
                final Photo photo = plugin.getPhoto(mapId);
                if (photo == null) {
                        player.sendMessage("" + ChatColor.RED  + "This map is not a photo. To get a photo, visit");
                        player.sendMessage("" + ChatColor.BLUE + "http://www.winthier.com/contributions");
                        return;
                }
                if (!plugin.photosConfig.hasPlayerMap(player, mapId)) {
                        player.sendMessage("" + ChatColor.RED  + "You don't own this map. To get a photo, visit");
                        player.sendMessage("" + ChatColor.BLUE + "http://www.winthier.com/contributions");
                        return;
                }
                if (!player.hasPermission("photos.override.loadcooldown")) {
                        Long tmp = loadCooldowns.get(player.getName());
                        long lastTime = tmp == null ? 0 : tmp;
                        long currentTime = System.currentTimeMillis() / 1000;
                        long remainTime = plugin.loadCooldown - (currentTime - lastTime);
                        if (remainTime > 0) {
                                player.sendMessage("" + ChatColor.RED  + "You have to wait " + remainTime + " more seconds before loading another image");
                                return;
                        }
                        loadCooldowns.put(player.getName(), currentTime);
                }
                new BukkitRunnable() {
                        public void run() {
                                if (!photo.loadURL(url, player)) {
                                        Util.sendAsyncMessage(player, "" + ChatColor.RED + "Can't load URL: " + url);
                                        return;
                                }
                                Util.sendAsyncMessage(player, "" + ChatColor.GREEN + "Image loaded: " + url);
                                photo.save();
                        }
                }.runTaskAsynchronously(plugin);
        }

        public void rename(Player player, String name) {
                ItemStack item = player.getItemInHand();
                if (item == null || item.getType() != Material.MAP) {
                        player.sendMessage("" + ChatColor.RED + "You must hold the photo you want to rename");
                        return;
                }
                short mapId = item.getDurability();
                final Photo photo = plugin.getPhoto(mapId);
                if (photo == null) {
                        player.sendMessage("" + ChatColor.RED  + "This map is not a photo. To get a photo, visit");
                        player.sendMessage("" + ChatColor.BLUE + "http://www.winthier.com/contributions");
                        return;
                }
                if (!plugin.photosConfig.hasPlayerMap(player, mapId)) {
                        player.sendMessage("" + ChatColor.RED  + "You don't own this map. To get a photo, visit");
                        player.sendMessage("" + ChatColor.BLUE + "http://www.winthier.com/contributions");
                        return;
                }
                photo.setName(name);
                plugin.photosConfig.saveConfig();
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName("" + ChatColor.GOLD + name);
                item.setItemMeta(meta);
                player.sendMessage("" + ChatColor.GREEN + "Name changed. Purchase a new copy by typing \"/photo menu\"");
        }

        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String args[]) {
                Player player = null;
                if (sender instanceof Player) player = (Player)sender;
                if (args.length == 2 && args[0].equals("load")) {
                        if (player == null) return false;
                        load(player, args[1]);
                        return true;
                }
                if (args.length >= 1 && args[0].equals("create")) {
                        if (player == null) return false;
                        String name = "Photo";
                        if (args.length > 1) {
                                StringBuilder sb = new StringBuilder(args[1]);
                                for (int i = 2; i < args.length; ++i) {
                                        sb.append(" ").append(args[i]);
                                }
                                if (sb.length() > 32) {
                                        player.sendMessage("" + ChatColor.RED + "Name is too long");
                                        return true;
                                }
                                name = sb.toString();
                        }
                        plugin.createPhoto(player, name);
                        return true;
                }
                if (args.length > 1 && args[0].equals("rename")) {
                        if (player == null) return false;
                        StringBuilder sb = new StringBuilder(args[1]);
                        for (int i = 2; i < args.length; ++i) {
                                sb.append(" ").append(args[i]);
                        }
                        if (sb.length() > 32) {
                                player.sendMessage("" + ChatColor.RED + "Name is too long");
                                return true;
                        }
                        String name = sb.toString();
                        rename(player, name);
                        return true;
                }
                if (args.length == 1 && args[0].equals("menu")) {
                        if (player == null) return false;
                        plugin.photosMenu.openMenu(player);
                        return true;
                }
                if (args.length == 1 && args[0].equals("reload")) {
                        if (!sender.hasPermission("photos.admin")) {
                                sender.sendMessage("" + ChatColor.RED + "You don't have permission!");
                                return true;
                        }
                        plugin.load();
                        sender.sendMessage("Configuraton reloaded");
                        return true;
                }
                if (args.length == 1 && args[0].equals("save")) {
                        if (!sender.hasPermission("photos.admin")) {
                                sender.sendMessage("" + ChatColor.RED + "You don't have permission!");
                                return true;
                        }
                        plugin.save();
                        sender.sendMessage("Configuraton saved");
                        return true;
                }
                if (args.length == 3 && args[0].equals("grant")) {
                        if (!sender.hasPermission("photos.admin")) {
                                sender.sendMessage("" + ChatColor.RED + "You don't have permission!");
                                return true;
                        }
                        String name = args[1];
                        Integer amount = null;
                        try { amount = Integer.parseInt(args[2]); } catch (NumberFormatException e) {}
                        if (amount == null || amount <= 0) {
                                sender.sendMessage("" + ChatColor.RED + "[Photos] Positive number exptected: " + args[2]);
                                return true;
                        }
                        plugin.photosConfig.addPlayerBlanks(name, amount);
                        plugin.photosConfig.saveConfig();
                        sender.sendMessage("" + ChatColor.GREEN + "[Photos] Granted " + name + " " + amount + " blank photos");
                        return true;
                }
                sender.sendMessage("" + ChatColor.YELLOW + "Usage: /photo [args...]");
                if (player != null) sender.sendMessage("" + ChatColor.BLUE   + "Blank Photos left: " + ChatColor.WHITE + plugin.photosConfig.getPlayerBlanks(player));
                sender.sendMessage("" + ChatColor.GRAY   + "--- SYNOPSIS ---");
                sender.sendMessage("" + ChatColor.YELLOW + "/photo menu");
                sender.sendMessage("" + ChatColor.GRAY   + "View a menu with all your photos.");
                sender.sendMessage("" + ChatColor.YELLOW + "/photo create [name]");
                sender.sendMessage("" + ChatColor.GRAY   + "Create a photo with the give name.");
                sender.sendMessage("" + ChatColor.YELLOW + "/photo rename <url>");
                sender.sendMessage("" + ChatColor.GRAY   + "Change the name of a photo.");
                sender.sendMessage("" + ChatColor.YELLOW + "/photo load <url>");
                sender.sendMessage("" + ChatColor.GRAY   + "Load a photo from the internet onto the map in your hand.");
                if (sender.hasPermission("photos.admin")) {
                        sender.sendMessage("" + ChatColor.YELLOW + "/photo grant <player> <#photos>");
                        sender.sendMessage("" + ChatColor.GRAY   + "Give a player more blank photos");
                        sender.sendMessage("" + ChatColor.YELLOW + "/photo reload");
                        sender.sendMessage("" + ChatColor.GRAY   + "Reload the configuration");
                        sender.sendMessage("" + ChatColor.YELLOW + "/photo save");
                        sender.sendMessage("" + ChatColor.GRAY   + "Save the configuration");
                }
                return true;
        }
}
