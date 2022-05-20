package com.winthier.photos;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandWarn;
import com.winthier.photos.legacy.LegacyDatabase;
import com.winthier.photos.legacy.LegacyPhoto;
import java.io.File;
import org.bukkit.command.CommandSender;
import org.bukkit.util.FileUtil;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

final class AdminCommand extends AbstractCommand<PhotosPlugin> {
    protected AdminCommand(final PhotosPlugin plugin) {
        super(plugin, "photoadmin");
    }

    @Override
    protected void onEnable() {
        rootNode.addChild("migrate")
            .description("Migrate legacy photos")
            .senderCaller(this::migrate);
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
}
