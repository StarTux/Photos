package com.winthier.photos;

import com.winthier.generic_events.GenericEvents;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@RequiredArgsConstructor
final class AdminCommand implements CommandExecutor {
    private final PhotosPlugin plugin;

    static final class AdminException extends Exception {
        AdminException(String message) {
            super(message);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) return false;
        try {
            return onCommand(sender, args[0], Arrays.copyOfRange(args, 1, args.length));
        } catch (AdminException ae) {
            sender.sendMessage("Error: " + ae.getMessage());
            return true;
        }
    }

    boolean onCommand(CommandSender sender, String cmd, String[] args) throws AdminException {
        switch (cmd) {
        case "list": {
            if (args.length != 1) return false;
            UUID uuid = parsePlayer(args[0]);
            List<Photo> photos = this.plugin.findPhotos(uuid);
            StringBuilder sb = new StringBuilder(args[0] + " has " + photos.size() + " Photos:");
            for (Photo photo: photos) sb.append(" ").append(photo.getId());
            sender.sendMessage(sb.toString());
            return true;
        }
        case "create": {
            if (args.length != 0 && args.length != 1) return false;
            if (args.length == 0) {
                Photo photo = this.plugin.createPhoto(null, "Admin Photo", 0);
                if (photo == null) {
                    sender.sendMessage("Failed to create new photo.");
                } else {
                    sender.sendMessage("Photo #" + photo.getId() + " created.");
                }
            } else {
                int id;
                id = parseInt(args[0]);
                Photo photo = this.plugin.createPhoto(id, null, "Admin Photo", 0);
                if (photo == null) {
                    sender.sendMessage("Failed to create Photo #" + id);
                } else {
                    sender.sendMessage("Photo #" + photo.getId() + " created.");
                }
            }
            return true;
        }
        case "info": {
            if (args.length != 1) return false;
            Photo photo = parsePhoto(args[0]);
            sender.sendMessage("Photo ID: " + photo.getId());
            sender.sendMessage("Owner: " + photo.getOwner());
            if (photo.getOwner() != null) {
                sender.sendMessage("Player: " + GenericEvents.cachedPlayerName(photo.getOwner()));
            }
            sender.sendMessage("Name: " + photo.getName());
            sender.sendMessage("Color: " + photo.getColor());
            return true;
        }
        case "transfer": {
            if (args.length != 1 && args.length != 2) return false;
            Photo photo = parsePhoto(args[0]);
            UUID owner = args.length >= 2 ? parsePlayer(args[1]) : null;
            photo.setOwner(owner);
            this.plugin.getDatabase().updatePhoto(photo);
            if (args.length >= 2) {
                sender.sendMessage("Transfered Photo #" + photo.getId() + " to " + args[1] + ".");
            } else {
                sender.sendMessage("Removed owner of Photo #" + photo.getId() + ".");
            }
            return true;
        }
        case "name": {
            if (args.length < 2) return false;
            Photo photo = parsePhoto(args[0]);
            StringBuilder sb = new StringBuilder(args[1]);
            for (int i = 2; i < args.length; i += 1) sb.append(" ").append(args[i]);
            photo.setName(sb.toString());
            this.plugin.getDatabase().updatePhoto(photo);
            sender.sendMessage("Changed name of Photo #" + photo.getId() + " to " + sb.toString() + ".");
            return true;
        }
        case "load": {
            if (args.length != 2) return false;
            Photo photo = parsePhoto(args[0]);
            URL url;
            try {
                url = new URL(args[1]);
            } catch (MalformedURLException murle) {
                throw new AdminException("Invalid URL: " + args[1]);
            }
            this.plugin.downloadPhotoAsync(photo, url, true, (result) -> {
                    sender.sendMessage("Download URL (" + url + ") to Photo #" + photo.getId() + ": " + result.status);
                });
            return true;
        }
        case "color": {
            if (args.length != 2) return false;
            Photo photo = parsePhoto(args[0]);
            int col = parseInt(args[1]);
            photo.setColor(col);
            this.plugin.getDatabase().updatePhoto(photo);
            sender.sendMessage("Changed name of Photo #" + photo.getId() + " to " + col + ".");
            return true;
        }
        case "grant": {
            if (args.length != 1) return false;
            UUID uuid = parsePlayer(args[0]);
            Photo photo = this.plugin.createPhoto(uuid, "Photo", 0);
            sender.sendMessage("Granted one new Photo to " + args[0] + ".");
            return true;
        }
        case "spawn": {
            if (args.length != 2) return false;
            UUID uuid = parsePlayer(args[0]);
            Photo photo = parsePhoto(args[1]);
            Player player = Bukkit.getServer().getPlayer(uuid);
            if (player == null) throw new AdminException("Player not online: " + args[0]);
            for (ItemStack drop: player.getInventory().addItem(this.plugin.createPhotoItem(photo)).values()) {
                player.getWorld().dropItem(player.getEyeLocation(), drop).setPickupDelay(0);
            }
            sender.sendMessage("Copy of Photo #" + photo.getId() + " given to " + args[0]);
            return true;
        }
        case "delete": {
            if (args.length != 1) return false;
            Photo photo = parsePhoto(args[0]);
            this.plugin.deletePhoto(photo);
            sender.sendMessage("Photo #" + photo.getId() + " deleted.");
            return true;
        }
        case "reload": {
            if (args.length != 0) return false;
            this.plugin.importConfig();
            this.plugin.loadPhotos();
            sender.sendMessage("Config and Photos reloaded.");
            return true;
        }
        default: return false;
        }
    }

    UUID parsePlayer(String arg) throws AdminException {
        UUID result = GenericEvents.cachedPlayerUuid(arg);
        if (result == null) throw new AdminException("Player not found: " + arg);
        return result;
    }

    Photo parsePhoto(String arg) throws AdminException {
        int id;
        try {
            id = Integer.parseInt(arg);
        } catch (NumberFormatException nfe) {
            throw new AdminException("Invalid number: " + arg);
        }
        Photo photo = this.plugin.findPhoto(id);
        if (photo == null) throw new AdminException("Photo with id not found: " + id);
        return photo;
    }

    int parseInt(String arg) throws AdminException {
        try {
            return Integer.parseInt(arg);
        } catch (NumberFormatException nfe) {
            throw new AdminException("Invalid number: " + arg);
        }
    }
}
