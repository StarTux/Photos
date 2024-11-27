package com.winthier.photos;

import java.awt.image.BufferedImage;
import org.bukkit.Bukkit;
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
    private final PhotoRuntime photo;
    private boolean paused = false;
    private boolean drawn = false;
    private BufferedImage image;
    private static boolean asyncLoading = false;
    private static boolean drawImageThisTick = false;

    protected PhotoRenderer(final PhotosPlugin plugin, final PhotoRuntime photo) {
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
            plugin.loadImageAsync(photo, this::accept);
            asyncLoading = true;
            return;
        }
        canvas.drawImage(0, 0, image);
        drawImageThisTick = true;
        drawn = true;
        Bukkit.getScheduler().runTask(plugin, () -> player.sendMap(view));
    }

    protected void refresh() {
        paused = false;
        drawn = false;
        asyncLoading = false;
        image = null;
    }

    protected void accept(BufferedImage newImage) {
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

    protected static void onTick() {
        drawImageThisTick = false;
    }
}
