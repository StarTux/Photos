package com.winthier.photos;

import com.winthier.photos.sql.SQLPhoto;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.bukkit.map.MapView;

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
}
