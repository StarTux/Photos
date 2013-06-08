package com.winthier.photos;

import java.awt.image.BufferedImage;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.scheduler.BukkitRunnable;

public class PhotoRenderer extends MapRenderer {
        private final Photo photo;

        public PhotoRenderer(Photo photo) {
                super(false);
                this.photo = photo;
        }

        @Override
        public void initialize(MapView map) {}

        @Override
        public void render(MapView map, MapCanvas canvas, Player player) {
                if (!photo.loaded) {
                        photo.loaded = true;
                        new BukkitRunnable() {
                                public void run() {
                                        photo.load();
                                }
                        }.runTaskAsynchronously(PhotosPlugin.getInstance());
                }
                if (!photo.dirty) return;
                BufferedImage image = photo.getImage();
                if (image == null) return;
                canvas.drawImage(0, 0, image);
                photo.dirty = false;
                photo.clearImage();
        }
}
