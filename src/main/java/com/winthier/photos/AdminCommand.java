package com.winthier.photos;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandArgCompleter;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.item.photo.Photo;
import com.winthier.photos.legacy.LegacyDatabase;
import com.winthier.photos.legacy.LegacyPhoto;
import com.winthier.photos.sql.SQLPhoto;
import com.winthier.playercache.PlayerCache;
import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.FileUtil;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.noSeparators;
import static net.kyori.adventure.text.format.NamedTextColor.*;

final class AdminCommand extends AbstractCommand<PhotosPlugin> {
    protected AdminCommand(final PhotosPlugin plugin) {
        super(plugin, "photoadmin");
    }

    @Override
    protected void onEnable() {
        rootNode.addChild("hand").denyTabCompletion()
            .description("Inspect photo in hand")
            .playerCaller(this::hand);
        rootNode.addChild("info").arguments("<photoId>")
            .description("View photo info")
            .completers(CommandArgCompleter.integer(i -> i > 0))
            .senderCaller(this::info);
        rootNode.addChild("list").arguments("<player>")
            .description("List player photos")
            .completers(PlayerCache.NAME_COMPLETER)
            .senderCaller(this::list);
        rootNode.addChild("grant").arguments("<player>")
            .description("Give player a blank photo")
            .completers(PlayerCache.NAME_COMPLETER)
            .senderCaller(this::grant);
        rootNode.addChild("give").arguments("<player> <photoId>")
            .description("Give player a photo copy")
            .completers(PlayerCache.NAME_COMPLETER,
                        CommandArgCompleter.integer(i -> i > 0))
            .senderCaller(this::give);
        rootNode.addChild("migrate").denyTabCompletion()
            .description("Migrate legacy photos")
            .senderCaller(this::migrate);
        rootNode.addChild("transfer").arguments("<photoId> <player>")
            .description("Transfer photo")
            .completers(CommandArgCompleter.integer(i -> i > 0),
                        PlayerCache.NAME_COMPLETER)
            .senderCaller(this::transfer);
        rootNode.addChild("confiscate").arguments("<photoId>")
            .description("Take photo away from player")
            .completers(CommandArgCompleter.integer(i -> i > 0))
            .senderCaller(this::confiscate);
        rootNode.addChild("transferall").arguments("<from> <to>")
            .description("Account transfer")
            .completers(PlayerCache.NAME_COMPLETER,
                        PlayerCache.NAME_COMPLETER)
            .senderCaller(this::transferAll);
        rootNode.addChild("prune").arguments("<forreal>")
            .description("Prune empty photos")
            .senderCaller(this::prune);
    }

    private void hand(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!Mytems.PHOTO.isItem(item)) {
            throw new CommandWarn("No photo in hand");
        }
        int photoId = Photo.getPhotoId(item);
        PhotoRuntime photo = plugin.getPhotos().ofPhotoId(photoId);
        if (photo == null) throw new CommandWarn("Invalid photo!");
        player.sendMessage(photo.toComponent());
    }

    private boolean list(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        PlayerCache target = PlayerCache.require(args[0]);
        List<PhotoRuntime> photos = plugin.getPhotos().find(target.uuid);
        if (photos.isEmpty()) {
            throw new CommandWarn(target.name + " does not have any photos");
        }
        int count = photos.size();
        sender.sendMessage(text(target.name + " has " + count + " photo" + (count == 1 ? "" : "s"), AQUA));
        for (PhotoRuntime photo : photos) {
            sender.sendMessage(join(noSeparators(), text("- ", AQUA), photo.toComponent()));
        }
        return true;
    }

    private boolean info(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        int photoId = CommandArgCompleter.requireInt(args[0], i -> i > 0);
        PhotoRuntime photo = plugin.getPhotos().ofPhotoId(photoId);
        if (photo == null) {
            throw new CommandWarn("Photo not found: " + photoId);
        }
        sender.sendMessage(photo.toComponent());
        return true;
    }

    private boolean grant(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        PlayerCache target = PlayerCache.require(args[0]);
        PhotoRuntime photo = plugin.getPhotos().create(target.uuid, "Free Photo", PhotoColor.totallyRandom());
        sender.sendMessage(join(noSeparators(), text("Granted photo to " + target.name + ": ", AQUA), photo.toComponent()));
        return true;
    }

    private boolean give(CommandSender sender, String[] args) {
        if (args.length != 2) return false;
        PlayerCache target = PlayerCache.require(args[0]);
        Player player = Bukkit.getPlayer(target.uuid);
        if (player == null) throw new CommandWarn("Player not online: " + target.name);
        int photoId = CommandArgCompleter.requireInt(args[1], i -> i > 0);
        PhotoRuntime photo = plugin.getPhotos().ofPhotoId(photoId);
        if (photo == null || !photo.isReady()) {
            throw new CommandWarn("Photo not found: " + photoId);
        }
        ItemStack item = Photo.createItemStack(photo.getRow().getId());
        for (ItemStack drop : player.getInventory().addItem(item).values()) {
            player.getWorld().dropItem(player.getEyeLocation(), drop).setPickupDelay(0);
        }
        sender.sendMessage(join(noSeparators(), text("Gave copy to " + target.name + ": ", AQUA), photo.toComponent()));
        return true;
    }

    private void migrate(CommandSender sender) {
        LegacyDatabase legacyDatabase = new LegacyDatabase(plugin);
        if (!legacyDatabase.isValid()) {
            throw new CommandWarn("Legacy file does not exist");
        }
        int count = 0;
        for (LegacyPhoto legacyPhoto : legacyDatabase.loadPhotos()) {
            PhotoRuntime photo = plugin.getPhotos().createLegacy(legacyPhoto.getId(),
                                                                 legacyPhoto.getOwner(),
                                                                 legacyPhoto.getName(),
                                                                 legacyPhoto.getColor());
            File oldFile = new File(plugin.getDataFolder(), "photos/" + legacyPhoto.filename());
            if (!oldFile.exists()) {
                sender.sendMessage("File not found: " + oldFile);
            }
            File newFile = new File(plugin.getImageFolder(), photo.getRow().filename());
            FileUtil.copy(oldFile, newFile);
            count += 1;
        }
        legacyDatabase.invalidate();
        sender.sendMessage(text("Migrated " + count + " photos", AQUA));
    }

    private boolean transfer(CommandSender sender, String[] args) {
        if (args.length != 2) return false;
        int photoId = CommandArgCompleter.requireInt(args[0], i -> i > 0);
        PlayerCache target = PlayerCache.require(args[1]);
        PhotoRuntime photo = plugin.getPhotos().ofPhotoId(photoId);
        if (photo == null || !photo.isReady()) {
            throw new CommandWarn("Photo not found: " + photoId);
        }
        photo.getRow().setOwner(target.uuid);
        photo.getRow().setUpdated(new Date());
        plugin.getDatabase().updateAsync(photo.getRow(), null, "owner", "updated");
        sender.sendMessage(text("Photo " + photoId + " transferred to " + target.name, AQUA));
        return true;
    }

    private boolean confiscate(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        int photoId = CommandArgCompleter.requireInt(args[0], i -> i > 0);
        PhotoRuntime photo = plugin.getPhotos().ofPhotoId(photoId);
        if (photo == null || !photo.isReady()) {
            throw new CommandWarn("Photo not found: " + photoId);
        }
        UUID oldOwner = photo.getRow().getOwner();
        if (oldOwner == null) {
            throw new CommandWarn("Photo " + photoId + " does not belong to anyone!");
        }
        photo.getRow().setOwner(null);
        photo.getRow().setUpdated(new Date());
        plugin.getDatabase().updateAsync(photo.getRow(), null, "owner", "updated");
        sender.sendMessage(text("Confiscated photo " + photoId + " from " + PlayerCache.nameForUuid(oldOwner), AQUA));
        return true;
    }

    private boolean transferAll(CommandSender sender, String[] args) {
        if (args.length != 2) return false;
        PlayerCache from = PlayerCache.require(args[0]);
        PlayerCache to = PlayerCache.require(args[1]);
        Date now = new Date();
        int count = plugin.getDatabase().update(SQLPhoto.class)
            .set("owner", to.uuid)
            .set("updated", now)
            .where(c -> c.eq("owner", from.uuid))
            .sync();
        if (count == 0) {
            throw new CommandWarn(from.name + " does not have any photos");
        }
        plugin.getPhotos().update();
        sender.sendMessage(text(count + " photo(s) transferred from " + from.name + " to " + to.name, AQUA));
        return true;
    }

    private boolean prune(CommandSender sender, String[] args) {
        boolean forReal = false;
        if (args.length == 1) {
            if (args[0].equals("forreal")) {
                forReal = true;
            } else {
                return false;
            }
        } else if (args.length != 0) {
            return false;
        }
        int total = 0;
        int blank = 0;
        for (SQLPhoto row : plugin.getDatabase().find(SQLPhoto.class).findList()) {
            total += 1;
            File file = new File(plugin.getImageFolder(), row.filename());
            if (!file.exists()) {
                blank += 1;
                if (forReal) plugin.getDatabase().delete(row);
            }
        }
        int local = 0;
        if (forReal) {
            local = plugin.getPhotos().pruneLocal();
        }
        if (blank == 0 && local == 0) {
            throw new CommandWarn("No blank photos were found");
        }
        if (forReal) {
            sender.sendMessage(text("Deleted " + blank + "/" + total + " blank photos, prunded " + local, YELLOW));
        } else {
            sender.sendMessage(text("Found " + blank + "/" + total + " blank photos", AQUA));
        }
        return true;
    }
}
