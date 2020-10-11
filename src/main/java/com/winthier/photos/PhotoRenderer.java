package com.winthier.photos;

import java.awt.image.BufferedImage;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

/**
 * A simple renderer which is tasked with rendering one Photos on its
 * map.
 */
final class PhotoRenderer extends MapRenderer {
    private final PhotosPlugin plugin;
    private final Photo photo;
    private boolean paused = false;
    private boolean drawn = false;
    private BufferedImage image = null;
    private static boolean asyncLoading = false;
    private static boolean drawImageThisTick = false;

    PhotoRenderer(final PhotosPlugin plugin, final Photo photo) {
        super(false);
        this.plugin = plugin;
        this.photo = photo;
    }

    @Override
    public void initialize(MapView map) { }

    /**
     * This method gets spammed a lot.
     */
    @Override
    public void render(MapView view, MapCanvas canvas, Player player) {
        if (paused || drawn || asyncLoading || drawImageThisTick) return;
        if (image == null) {
            paused = true;
            plugin.loadImageAsync(photo.filename(), this::accept);
            asyncLoading = true;
            return;
        }
        canvas.drawImage(0, 0, image);
        drawImageThisTick = true;
        drawn = true;
    }

    void accept(BufferedImage newImage) {
        asyncLoading = false;
        if (newImage == null) {
            image = plugin.getDefaultImage();
        } else {
            image = newImage;
        }
        if (image == null) return;
        paused = false;
        drawn = false;
    }

    public static void onTick() {
        drawImageThisTick = false;
    }
}
