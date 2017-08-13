package com.winthier.photos;

import com.winthier.playercache.PlayerCache;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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
    private final Map<UUID, Long> loadCooldowns = new HashMap<>();
    private final Map<UUID, UUID> buyCodes = new HashMap<>();

    public PhotoCommand(PhotosPlugin plugin) {
        this.plugin = plugin;
    }

    public void load(final Player player, final String url) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() != Material.MAP) {
            player.sendMessage("" + ChatColor.RED + "You must hold a photo in your main hand");
            return;
        }
        short mapId = item.getDurability();
        final Photo photo = plugin.getPhoto(mapId);
        if (photo == null) {
            player.sendMessage("" + ChatColor.RED  + "This map is not a photo.");
            return;
        }
        if (!plugin.photosConfig.hasPlayerMap(player, mapId)) {
            player.sendMessage("" + ChatColor.RED  + "You do not own this map.");
            return;
        }
        if (!player.hasPermission("photos.override.loadcooldown")) {
            Long tmp = loadCooldowns.get(player.getUniqueId());
            long lastTime = tmp == null ? 0 : tmp;
            long currentTime = System.currentTimeMillis() / 1000;
            long remainTime = plugin.loadCooldown - (currentTime - lastTime);
            if (remainTime > 0) {
                player.sendMessage("" + ChatColor.RED  + "You have to wait " + remainTime + " more seconds before loading another image");
                return;
            }
            loadCooldowns.put(player.getUniqueId(), currentTime);
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
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() != Material.MAP) {
            player.sendMessage("" + ChatColor.RED + "You must hold the photo in your main hand");
            return;
        }
        short mapId = item.getDurability();
        final Photo photo = plugin.getPhoto(mapId);
        if (photo == null) {
            player.sendMessage("" + ChatColor.RED  + "This map is not a photo.");
            return;
        }
        if (!plugin.photosConfig.hasPlayerMap(player, mapId)) {
            player.sendMessage("" + ChatColor.RED  + "You do not own this map.");
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
            UUID uuid = PlayerCache.uuidForName(name);
            if (uuid == null) {
                sender.sendMessage("" + ChatColor.RED + "[Photos] Player not found: " + name);
                return true;
            }
            name = PlayerCache.nameForUuid(uuid);
            Integer amount = null;
            try { amount = Integer.parseInt(args[2]); } catch (NumberFormatException e) {}
            if (amount == null || amount <= 0) {
                sender.sendMessage("" + ChatColor.RED + "[Photos] Positive number exptected: " + args[2]);
                return true;
            }
            plugin.photosConfig.addPlayerBlanks(uuid, amount);
            plugin.photosConfig.saveConfig();
            sender.sendMessage("" + ChatColor.GREEN + "[Photos] Granted " + name + " " + amount + " blank photos");
            return true;
        }
        if ((args.length == 1 || args.length == 2) && args[0].equals("buy")) {
            String moneyFormat = plugin.economy.format(plugin.blankPrice);
            if (args.length == 1) {
                if (!plugin.economy.has(player, plugin.blankPrice)) {
                    player.sendMessage("" + ChatColor.RED + "You cannot afford " + moneyFormat + ".");
                    return true;
                }
                UUID buyCode = UUID.randomUUID();
                buyCodes.put(player.getUniqueId(), buyCode);
                Msg.raw(player,
                        Msg.format("&7Confirm the purchase for &r%s&7: ", moneyFormat),
                        Msg.button("[Confirm]",
                                   null,
                                   "&9Confirm payment of " + moneyFormat + ".",
                                   "/photo buy " + buyCode,
                                   ChatColor.GOLD));
                return true;
            } else if (args.length == 2) {
                UUID buyCode = buyCodes.remove(player.getUniqueId());
                if (buyCode == null) return false;
                UUID code;
                try {
                    code = UUID.fromString(args[1]);
                } catch (IllegalArgumentException iae) {
                    return false;
                }
                if (!buyCode.equals(code)) return false;
                if (!plugin.economy.has(player, plugin.blankPrice)
                    || !plugin.economy.withdrawPlayer(player, plugin.blankPrice).transactionSuccess()) {
                    player.sendMessage("" + ChatColor.RED + "You cannot afford " + moneyFormat + ".");
                    return true;
                }
                UUID uuid = player.getUniqueId();
                plugin.photosConfig.addPlayerBlanks(uuid, 1);
                plugin.photosConfig.saveConfig();
                plugin.getLogger().info(player.getName() + " bought a blank photo for " + moneyFormat);
                sender.sendMessage("" + ChatColor.GREEN + "[Photos] Bought a blank photo. You now have " + plugin.photosConfig.getPlayerBlanks(uuid) + " blanks.");
                return true;
            }
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
        sender.sendMessage("" + ChatColor.YELLOW + "/photo buy");
        sender.sendMessage("" + ChatColor.GRAY   + "Buy a new blank photo for " + plugin.economy.format(plugin.blankPrice) + ".");
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
