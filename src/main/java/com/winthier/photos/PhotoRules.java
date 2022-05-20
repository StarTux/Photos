package com.winthier.photos;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.noSeparators;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.*;

public final class PhotoRules {
    private static final TextColor HIGHLIGHT = DARK_RED;

    public static final List<Component> PAGES = List.of(new Component[] {
            join(noSeparators(),
                 text("Photo Rules", HIGHLIGHT, BOLD),
                 newline(),
                 newline(),
                 text("All server rules apply to Photos!, WHITE")),
            join(noSeparators(),
                 text("No "),
                 text("inappropriate", HIGHLIGHT),
                 text("content, including but not limited to "),
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
                 text("No "),
                 text("upsetting", HIGHLIGHT),
                 text("content, including but not limited to "),
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
                 text("Rules apply at all times, whether your Photo is "),
                 text("displayed", HIGHLIGHT),
                 text("or not."),
                 newline(),
                 newline(),
                 text("Violations will result in a "),
                 text("server ban", HIGHLIGHT),
                 text(". Cavetale admins decide what is or is not appropriate.")),
        });

    private PhotoRules() { };
}
