package com.winthier.photos;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandArgCompleter;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.item.photo.Photo;
import com.winthier.photos.sql.SQLPhoto;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import org.bukkit.Color;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import static com.cavetale.mytems.item.photo.Photo.SEPIA;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.noSeparators;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextColor.color;

/**
 * Executor for the `/photo` command.
 * This command is run by normal users, so all input has to be double
 * checked for potential malicious behavior.
 */
final class PhotoCommand extends AbstractCommand<PhotosPlugin> {
    protected PhotoCommand(final PhotosPlugin plugin) {
        super(plugin, "photo");
    }

    @Override
    protected void onEnable() {
        rootNode.description("Photo commands")
            .playerCaller(this::photos);
        rootNode.addChild("load").arguments("<url>")
            .description("Load a new picture from the internet")
            .completers(CommandArgCompleter.EMPTY)
            .playerCaller(this::load);
        rootNode.addChild("name").arguments("<name>")
            .description("Rename a photo")
            .completers(CommandArgCompleter.EMPTY)
            .playerCaller(this::name);
        rootNode.addChild("color").arguments("<color>")
            .description("Change photo color")
            .completers(CommandArgCompleter.enumLowerList(PhotoColor.class))
            .playerCaller(this::setColor);
        rootNode.addChild("accept").denyTabCompletion()
            .hidden(true)
            .playerCaller(this::accept);
        rootNode.addChild("help").denyTabCompletion()
            .description("Print help")
            .senderCaller((s, a) -> false);
    }

    private void photos(Player player) {
        new PhotosMenu(plugin, plugin.getPhotos().find(player.getUniqueId())).open(player);
    }

    private boolean load(Player player, String[] args) {
        if (args.length != 1) return false;
        ItemStack item = photoInHand(player);
        PhotoRuntime photo = photoOfItem(player, item);
        URL url = parseURL(args[0]);
        putOnCooldown(player);
        player.sendMessage(text("Loading " + url + "...", color(SEPIA)));
        plugin.downloadPhotoAsync(photo, url, false, (result) -> {
                if (result.status().isSuccessful()) {
                    photo.getRow().setUpdated(new Date());
                    plugin.getDatabase().updateAsync(photo.getRow(), null, "updated");
                }
                acceptDownload(player, photo, url, result);
            });
        return true;
    }

    private boolean name(Player player, String[] args) {
        if (args.length == 0) return false;
        ItemStack item = photoInHand(player);
        PhotoRuntime photo = photoOfItem(player, item);
        SQLPhoto row = photo.getRow();
        String newName = String.join(" ", args);
        if (newName.length() > 127) {
            throw new CommandWarn("Name cannot be longer than 127 characters.");
        }
        row.setName(newName);
        row.setUpdated(new Date());
        plugin.getDatabase().updateAsync(photo, null, "name", "updated");
        updatePhotoItem(photo, item);
        player.sendMessage(join(noSeparators(), text("Updated name: ", color(SEPIA)), text(newName, WHITE)));
        return true;
    }

    private boolean setColor(Player player, String[] args) {
        if (args.length != 1 && args.length != 3) return false;
        ItemStack item = photoInHand(player);
        PhotoRuntime photo = photoOfItem(player, item);
        SQLPhoto row = photo.getRow();
        Color color;
        if (args.length == 3) {
            int r = parseColorComponent(args[0]);
            int g = parseColorComponent(args[1]);
            int b = parseColorComponent(args[2]);
            color = Color.fromRGB(r, g, b);
        } else {
            color = parseColor(args[0]);
        }
        row.setColor(color.asRGB());
        row.setUpdated(new Date());
        plugin.getDatabase().updateAsync(row, null, "color", "updated");
        updatePhotoItem(photo, item);
        player.sendMessage(text("Photo color updated", color(row.getColor())));
        return true;
    }

    private void accept(Player player) {
        player.sendMessage(text("Thank you for accepting the rules!", GREEN));
        //plugin.getDatabase().consent(player.getUniqueId());
        //new PhotosMenu(plugin).open(player);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 0.25f, 2.0f);
    }

    /**
     * This is the callback for the `/photo load URL` command. See
     * PhotoCommand#photoCommand(Player, String, String[]).
     */
    private void acceptDownload(Player player, PhotoRuntime photo, URL url, DownloadResult result) {
        switch (result.status()) {
        case SUCCESS: {
            player.sendMessage(text("Image successfully downloaded. Please wait a minute for the photo to update.", GREEN));
            break;
        }
        case NOT_FOUND: {
            player.sendMessage(text("File not found: " + url, RED));
            break;
        }
        case TOO_LARGE: {
            player.sendMessage(text("This file is too large!", RED));
            break;
        }
        case NOT_IMAGE: {
            player.sendMessage(text("This file is not an image!", RED));
            break;
        }
        case NOSAVE: {
            player.sendMessage(text("An internal error occured. Please contact an administrator.", RED));
            break;
        }
        case UNKNOWN: default: {
            if (result.exception() != null) {
                player.sendMessage(text("An error occured: " + result.exception(), RED));
                result.exception().printStackTrace();
            } else {
                player.sendMessage(text("An unknown error occured", RED));
            }
            break;
        }
        }
    }

    /**
     * Return the map a player is holding or throw CommandWarn.
     */
    protected ItemStack photoInHand(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || !Mytems.PHOTO.isItem(item)) {
            throw new CommandWarn("Hold a photo in your hand");
        }
        return item;
    }

    /**
     * Return the Photo instance which belongs to the given map item,
     * or throw CommandWarn.
     */
    protected PhotoRuntime photoOfItem(Player player, ItemStack item) {
        if (!Mytems.PHOTO.isItem(item)) {
            throw new CommandWarn("Item is not a photo");
        }
        int photoId = Photo.getPhotoId(item);
        if (photoId == 0) {
            throw new CommandWarn("This map is not a valid photo");
        }
        PhotoRuntime photo = plugin.getPhotos().ofPhotoId(photoId);
        if (photo == null || !photo.isReady()) {
            throw new CommandWarn("This map is not a valid photo");
        }
        if (!player.getUniqueId().equals(photo.getRow().getOwner())) {
            throw new CommandWarn("This photo does not belong to you.");
        }
        return photo;
    }

    protected void updatePhotoItem(PhotoRuntime photo, ItemStack item) {
        ItemStack newItem = Photo.createItemStack(photo.getRow().getId());
        item.setItemMeta(newItem.getItemMeta());
    }

    /**
     * Parse a URL or throw CommandWarn.
     */
    protected URL parseURL(String arg) {
        if (arg == null) throw new NullPointerException("arg cannot be null");
        try {
            return new URL(arg);
        } catch (MalformedURLException murle) {
            throw new CommandWarn("Invalid URL: " + arg + ".");
        }
    }

    private static final String META_COOLDOWN = "photos:cooldown";

    /**
     * Put a player on cooldown or throw CommandWarn if they are already
     * on cooldown.
     */
    protected void putOnCooldown(Player player) {
        long cd = 0L;
        for (MetadataValue meta : player.getMetadata(META_COOLDOWN)) {
            if (meta.getOwningPlugin() == plugin) {
                cd = meta.asLong();
                break;
            }
        }
        long now = System.nanoTime();
        if (cd > 0 && now < cd) {
            long wait = (cd - now) / 1000000000;
            throw new CommandWarn("You must wait " + wait + " seconds.");
        }
        cd = now + (long) plugin.getLoadCooldown() * 1000000000;
        player.setMetadata(META_COOLDOWN, new FixedMetadataValue(plugin, cd));
    }

    /**
     * Parse a number bettween from 0 to 255 or throw CommandWarn.
     */
    protected int parseColorComponent(String arg) {
        if (arg == null) throw new NullPointerException("arg cannot be null");
        int result;
        try {
            result = Integer.parseInt(arg);
        } catch (NumberFormatException nfe) {
            throw new CommandWarn("Number expected: " + arg + ".");
        }
        if (result < 0 || result > 255) {
            throw new CommandWarn("Color component must be between 0 and 255.");
        }
        return result;
    }

    /**
     * Parse a Color or throw CommandWarn.
     */
    protected Color parseColor(String arg) {
        if (arg == null) throw new NullPointerException("arg cannot be null");
        Color result;
        try {
            PhotoColor photoColor = PhotoColor.valueOf(arg.toUpperCase());
            return photoColor.color;
        } catch (IllegalArgumentException iae) {
            throw new CommandWarn("Unknown color: " + arg + ".");
        }
    }
}
