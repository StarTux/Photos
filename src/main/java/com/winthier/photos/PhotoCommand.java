package com.winthier.photos;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

/**
 * Executor for the `/photo` command.
 * This command is run by normal users, so all input has to be double
 * checked for potential malicious behavior.
 */
final class PhotoCommand implements TabExecutor {
    public final PhotosPlugin plugin;
    static final String META_COOLDOWN = "photos.cooldown";

    PhotoCommand(PhotosPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * An exception to be thrown whenever the command sender is to be
     * presented with a command synopsis.  This is typically the case
     * when they entered invalid syntax.
     */
    static final class UsageError extends Exception {
    }

    /**
     * An exception to be thrown when the user made a mistake and
     * needs to be told a final message before the command execution
     * is aborted.
     */
    static final class UserError extends Exception {
        UserError(String message) {
            super(message);
        }
    }

    /**
     * When `/photo` is used without any aruments, open the photos GUI.
     * If there are any arguments present, try to parse them, catch
     * thrown Usage or UserErrors, and react accordingly.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Player expected");
            return true;
        }
        Player player = (Player)sender;
        if (args.length == 0) {
            new PhotosMenu(plugin).open(player);
            player.playSound(player.getEyeLocation(), Sound.BLOCK_CHEST_OPEN, SoundCategory.MASTER, 0.25f, 2.0f);
            return true;
        } else {
            try {
                photoCommand(player, args[0], Arrays.copyOfRange(args, 1, args.length));
            } catch (UserError user) {
                sender.sendMessage(ChatColor.RED + user.getMessage());
            } catch (UsageError usage) {
                return false;
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return null;
        Player player = (Player)sender;
        ItemStack item = player.getInventory().getItemInHand();
        Photo photo = null;
        if (item != null && item.getType() == Material.FILLED_MAP) photo = plugin.findPhoto(item);
        if (args.length == 0) return null;
        if (args.length == 1) {
            return tabComplete(args[0], Arrays.asList("load", "name", "color"));
        }
        if (args[0].equals("color")) {
            Color color = photo != null ? Color.fromRGB(photo.getColor()) : Color.fromRGB(0);
            switch (args.length) {
            case 2:
                List<String> tabs = new ArrayList<>();
                tabs.add("" + color.getRed());
                for (PhotoColor pc: PhotoColor.values()) tabs.add(pc.name().toLowerCase());
                return tabComplete(args[1], tabs);
            case 3: return tabComplete(args[2], Arrays.asList("" + color.getGreen()));
            case 4: return tabComplete(args[3], Arrays.asList("" + color.getBlue()));
            default: return Collections.emptyList();
            }
        }
        if (args.length == 2) {
            switch (args[0]) {
            case "load": return tabComplete(args[1], Arrays.asList(plugin.getDefaultDownloadURL()));
            case "name": return tabComplete(args[1], Arrays.asList(photo != null ? photo.getName() : "Photo"));
            default: return Collections.emptyList();
            }
        }
        return null;
    }

    List<String> tabComplete(String arg, List<String> args) {
        return args.stream().filter(i -> i.startsWith(arg)).collect(Collectors.toList());
    }

    void suggestPhotoCommands(Player player, Photo photo) {
        Color color = Color.fromRGB(photo.getColor());
        player.spigot().sendMessage(new ComponentBuilder("Photo").color(ChatColor.LIGHT_PURPLE)
                                    .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/photo"))
                                    .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(ChatColor.LIGHT_PURPLE + "/photo\n" + ChatColor.WHITE + "Open the Photos Menu.")))
                                    .append("  ").reset()
                                    .append("[Load]").color(ChatColor.BLUE)
                                    .event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/photo load "))
                                    .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(ChatColor.LIGHT_PURPLE + "/photo load " + ChatColor.ITALIC + "URL\n" + ChatColor.WHITE + "Load a new picture from the internet.")))
                                    .append("  ").reset()
                                    .append("[Name]").color(ChatColor.YELLOW)
                                    .event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/photo name " + photo.getName()))
                                    .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(ChatColor.LIGHT_PURPLE + "/photo rename " + ChatColor.ITALIC + "NAME\n" + ChatColor.WHITE + "Change the name of this Photo.")))
                                    .append("  ").reset()
                                    .append("[Color]").color(ChatColor.LIGHT_PURPLE)
                                    .event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/photo color " + color.getRed() + " " + color.getGreen() + " " + color.getBlue()))
                                    .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(ChatColor.LIGHT_PURPLE + "/photo color " + ChatColor.RED + ChatColor.ITALIC + "RED " + ChatColor.GREEN + ChatColor.ITALIC + "GREEN " + ChatColor.BLUE + ChatColor.ITALIC + "BLUE\n" + ChatColor.LIGHT_PURPLE + "/photo color " + ChatColor.ITALIC + "COLOR\n" + ChatColor.WHITE + "Change the item color of this Photo.")))
                                    .create());
    }

    void photoCommand(Player player, String cmd, String[] args) throws UsageError, UserError {
        switch (cmd) {
        case "load": {
            if (args.length != 1) throw new UsageError();
            ItemStack item = mapInHand(player);
            Photo photo = photoOfMap(player, item);
            URL url = parseURL(args[0]);
            putOnCooldown(player);
            player.sendMessage(ChatColor.LIGHT_PURPLE + "Loading " + url + "...");
            plugin.downloadPhotoAsync(photo, url, false, (result) -> this.acceptDownload(player, photo, url, result));
            break;
        }
        case "name": {
            if (args.length == 0) throw new UsageError();
            ItemStack item = mapInHand(player);
            Photo photo = photoOfMap(player, item);
            String newName = compilePhotoName(args);
            photo.setName(newName);
            plugin.getDatabase().updatePhoto(photo);
            plugin.updatePhotoItem(item, photo);
            player.sendMessage(ChatColor.LIGHT_PURPLE + "Updated name: " + ChatColor.WHITE + ChatColor.ITALIC + newName + ChatColor.LIGHT_PURPLE + ".");
            break;
        }
        case "color": {
            if (args.length != 1 && args.length != 3) throw new UsageError();
            ItemStack item = mapInHand(player);
            Photo photo = photoOfMap(player, item);
            Color color;
            if (args.length == 3) {
                int r = parseColorComponent(args[0]);
                int g = parseColorComponent(args[1]);
                int b = parseColorComponent(args[2]);
                color = Color.fromRGB(r, g, b);
            } else {
                color = parseColor(args[0]);
            }
            photo.setColor(color.asRGB());
            plugin.getDatabase().updatePhoto(photo);
            plugin.updatePhotoItem(item, photo);
            player.sendMessage(ChatColor.LIGHT_PURPLE + "Photo color updated.");
            break;
        }
        default: throw new UsageError();
        }
    }

    /**
     * This is the callback for the `/photo load URL` command. See
     * PhotoCommand#photoCommand(Player, String, String[]).
     */
    private void acceptDownload(Player player, Photo photo, URL url, PhotosPlugin.DownloadResult result) {
        switch (result.status) {
        case SUCCESS: {
            player.sendMessage(ChatColor.LIGHT_PURPLE + "Photo downloaded from " + url + ".");
            break;
        }
        case NOT_FOUND: {
            player.sendMessage(ChatColor.RED + "File not found: " + url + ".");
            break;
        }
        case TOO_LARGE: {
            player.sendMessage(ChatColor.RED + "This file is too large!");
            break;
        }
        case NOT_IMAGE: {
            player.sendMessage(ChatColor.RED + "This file is not an image!");
            break;
        }
        case NOSAVE: {
            player.sendMessage(ChatColor.RED + "An internal error occured. Please contact an administrator.");
            break;
        }
        case UNKNOWN: default: {
            if (result.exception != null) {
                player.sendMessage(ChatColor.RED + "An error occured: " + result.exception + ".");
                result.exception.printStackTrace();
            } else {
                player.sendMessage(ChatColor.RED + "An unknown error occured.");
            }
            break;
        }
        }
    }

    /**
     * Return the map a player is holding or throw UserError.
     */
    ItemStack mapInHand(Player player) throws UserError {
        if (player == null) throw new NullPointerException("Player cannot be null");
        ItemStack result = player.getInventory().getItemInMainHand();
        if (result == null || result.getType() != Material.FILLED_MAP) throw new UserError("Hold a photo in your hand.");
        return result;
    }

    /**
     * Return the Photo instance which belongs to the given map item,
     * or throw UserError.
     */
    Photo photoOfMap(Player player, ItemStack item) throws UserError {
        if (player == null) throw new NullPointerException("Player cannot be null");
        if (item == null || item.getType() != Material.FILLED_MAP) throw new IllegalArgumentException("Item is not a map");
        Photo result = plugin.findPhoto(item);
        if (result == null) throw new UserError("This map is not a photo.");
        if (!player.getUniqueId().equals(result.getOwner())) throw new UserError("This photo does not belong to you.");
        return result;
    }

    /**
     * Parse a URL or throw UserError.
     */
    URL parseURL(String arg) throws UserError {
        if (arg == null) throw new NullPointerException("arg cannot be null");
        try {
            return new URL(arg);
        } catch (MalformedURLException murle) {
            throw new UserError("Invalid URL: " + arg + ".");
        }
    }

    /**
     * Put a player on cooldown or throw UserError if they are already
     * on cooldown.
     */
    void putOnCooldown(Player player) throws UserError {
        long cd = 0L;
        for (MetadataValue meta: player.getMetadata(META_COOLDOWN)) {
            if (meta.getOwningPlugin() == plugin) {
                cd = meta.asLong();
                break;
            }
        }
        long now = System.nanoTime();
        if (cd > 0 && now < cd) {
            long wait = (cd - now) / 1000000000;
            throw new UserError("You must wait " + wait + " seconds.");
        }
        cd = now + (long)plugin.getLoadCooldown() * 1000000000;
        player.setMetadata(META_COOLDOWN, new FixedMetadataValue(plugin, cd));
    }

    /**
     * Fold a list of arguments into the new Photo name or throw
     * UserError if antyhing is wrong with the name.
     */
    String compilePhotoName(String[] args) throws UserError {
        if (args.length == 0) throw new ArrayIndexOutOfBoundsException("args cannot be empty");
        StringBuilder sb = new StringBuilder(args[0]);
        for (int i = 1; i < args.length; i += 1) sb.append(" ").append(args[i]);
        String result = ChatColor.stripColor(sb.toString());
        if (result.length() > 127) throw new UserError("Name cannot be longer than 127 characters.");
        return result;
    }

    /**
     * Parse a number bettween from 0 to 255 or throw UserError.
     */
    int parseColorComponent(String arg) throws UserError {
        if (arg == null) throw new NullPointerException("arg cannot be null");
        int result;
        try {
            result = Integer.parseInt(arg);
        } catch (NumberFormatException nfe) {
            throw new UserError("Number expected: " + arg + ".");
        }
        if (result < 0 || result > 255) throw new UserError("Color component must be between 0 and 255.");
        return result;
    }

    /**
     * Parse a Color or throw UserError.
     */
    Color parseColor(String arg) throws UserError {
        if (arg == null) throw new NullPointerException("arg cannot be null");
        Color result;
        try {
            PhotoColor photoColor = PhotoColor.valueOf(arg.toUpperCase());
            return photoColor.color;
        } catch (IllegalArgumentException iae) {
            throw new UserError("Unknown color: " + arg + ".");
        }
    }
}
