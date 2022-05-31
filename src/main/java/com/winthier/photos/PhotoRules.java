package com.winthier.photos;

import com.cavetale.core.font.DefaultFont;
import com.cavetale.mytems.Mytems;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.noSeparators;
import static net.kyori.adventure.text.event.ClickEvent.runCommand;
import static net.kyori.adventure.text.event.HoverEvent.showText;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.*;

public final class PhotoRules {
    private static final TextColor HIGHLIGHT = DARK_BLUE;

    public static final List<Component> PAGES = List.of(new Component[] {
            join(noSeparators(),
                 text("Photo Rules", HIGHLIGHT, BOLD),
                 newline(),
                 newline(),
                 text("You must accept the rules before you proceed."),
                 newline(),
                 newline(),
                 Mytems.PHOTO.component, space(),
                 text("All server rules apply to Photos, see "),
                 text("/rules", HIGHLIGHT)
                 .hoverEvent(showText(text("/rules", GREEN)))
                 .clickEvent(runCommand("/rules")),
                 text(".")),
            join(noSeparators(),
                 Mytems.PHOTO.component, space(),
                 text("Rules may be updated any time without further notice."),
                 newline(),
                 newline(),
                 Mytems.PHOTO.component, space(),
                 text("You are expected to understand and apply "),
                 text("common sense", HIGHLIGHT),
                 text(".")),
            join(noSeparators(),
                 Mytems.PHOTO.component, space(),
                 text("No "),
                 text("inappropriate", HIGHLIGHT),
                 text(" content, including but not limited to "),
                 text("violence", HIGHLIGHT),
                 text(", "),
                 text("nudity", HIGHLIGHT),
                 text(", "),
                 text("drug use", HIGHLIGHT),
                 text(", "),
                 text("insults", HIGHLIGHT),
                 text("."),
                 newline(),
                 newline(),
                 Mytems.PHOTO.component, space(),
                 text("No "),
                 text("upsetting", HIGHLIGHT),
                 text(" content, including but not limited to "),
                 text("horror", HIGHLIGHT),
                 text(", "),
                 text("racism", HIGHLIGHT),
                 text(", "),
                 text("sexism", HIGHLIGHT),
                 text(", "),
                 text("politics"),
                 text(", "),
                 text("bigotry"),
                 text(".")),
            join(noSeparators(),
                 Mytems.PHOTO.component, space(),
                 text("Rules apply at all times, whether your Photo is "),
                 text("displayed", HIGHLIGHT),
                 text(" or not."),
                 newline(),
                 newline(),
                 Mytems.PHOTO.component, space(),
                 text("Violations will result in a "),
                 text("server ban", HIGHLIGHT),
                 text(". Cavetale admins decide what is or is not appropriate.")),
            join(noSeparators(),
                 text("You must accept the rules before you proceed."),
                 newline(),
                 newline(),
                 DefaultFont.ACCEPT_BUTTON.component
                 .hoverEvent(showText(text("Accept these rules", GREEN)))
                 .clickEvent(runCommand("/photo accept")),
                 space(),
                 DefaultFont.DECLINE_BUTTON.component
                 .hoverEvent(showText(text("No photos for you", RED)))
                 .clickEvent(runCommand("/photo decline"))),
        });

    public static ItemStack makeBook() {
        ItemStack item = new ItemStack(Material.WRITTEN_BOOK);
        item.editMeta(m -> {
                if (m instanceof BookMeta meta) {
                    meta.setTitle("Photo Rules");
                    meta.setAuthor("Cavetale");
                    meta.pages(PAGES);
                }
            });
        return item;
    }

    private PhotoRules() { };
}
