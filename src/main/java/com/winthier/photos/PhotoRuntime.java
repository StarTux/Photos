package com.winthier.photos;

import com.winthier.photos.sql.SQLPhoto;
import com.winthier.playercache.PlayerCache;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.bukkit.map.MapView;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.noSeparators;
import static net.kyori.adventure.text.JoinConfiguration.separator;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextColor.color;

@Getter @Setter @RequiredArgsConstructor
final class PhotoRuntime {
    private final int photoId; // informal
    private int mapId; // informal
    protected SQLPhoto row;
    protected PhotoRenderer renderer;
    protected MapView mapView;

    public boolean isReady() {
        return row != null
            && renderer != null
            && mapView != null;
    }

    public Component toComponent() {
        List<Component> list = new ArrayList<>();
        list.add(join(noSeparators(), text("photoId:", GRAY), text(photoId, WHITE)));
        list.add(join(noSeparators(), text("mapId:", GRAY), text(mapId, WHITE)));
        if (row != null) {
            String name = row.getOwner() != null ? PlayerCache.nameForUuid(row.getOwner()) : "-";
            list.add(join(noSeparators(), text("owner:", GRAY), text(name, WHITE)));
            list.add(join(noSeparators(), text("created:", GRAY), text(row.formatCreationDate(), WHITE)));
            list.add(join(noSeparators(), text("name:", GRAY), text("" + row.getName(), WHITE)));
            int hexColor = row.getHexColor();
            list.add(join(noSeparators(), text("color:", GRAY), text("0x" + Integer.toHexString(hexColor), color(hexColor))));
        } else {
            list.add(text("row=null", DARK_GRAY));
        }
        return join(separator(space()), list)
            .insertion("" + photoId);
    }
}
