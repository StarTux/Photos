package com.winthier.photos;

import com.cavetale.mytems.item.photo.Photo;
import com.cavetale.mytems.item.photo.PhotoData;
import com.winthier.photos.sql.SQLConsent;
import com.winthier.photos.sql.SQLPhoto;
import com.winthier.photos.util.Gui;
import com.winthier.sql.SQLDatabase;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import static java.awt.Color.HSBtoRGB;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB;

/**
 * The main plugin class.
 * An instance of this manages all Photos and configuration values,
 * and runs some utility functions to find or modify Photos, and
 * perform Input/Output via disk or network.
 */
@Getter
public final class PhotosPlugin extends JavaPlugin {
    private final double photoPrice = 1000.0;
    private final double copyPrice = 100.0;
    private final long loadCooldown = 10L;
    private final int maxFileSize = 1024 * 32;
    private final BufferedImage defaultImage = new BufferedImage(128, 128, TYPE_INT_ARGB);
    private final String defaultDownloadURL = "https://i.imgur.com/NNvWR6B.png";
    private PhotoCommand photoCommand = new PhotoCommand(this);
    private AdminCommand adminCommand = new AdminCommand(this);
    private final SQLDatabase database = new SQLDatabase(this);
    private Photos photos = new Photos(this);
    private File imageFolder;

    @Override
    public void onEnable() {
        imageFolder = new File(getDataFolder(), "images");
        imageFolder.mkdirs();
        database.registerTables(List.of(SQLPhoto.class, SQLConsent.class));
        if (!database.createAllTables()) {
            throw new IllegalStateException("Database setup failed");
        }
        photos.enable();
        photoCommand = new PhotoCommand(this);
        adminCommand = new AdminCommand(this);
        Gui.enable(this);
        photoCommand.enable();
        adminCommand.enable();
        getServer().getScheduler().runTaskTimer(this, PhotoRenderer::onTick, 1L, 1L);
        Photo.setPhotoDataGetter(this::getMytemsPhotoData);
        Photo.setPhotoIdGetter(this::mapIdToPhotoId);
        for (int y = 0; y < 128; y += 1) {
            for (int x = 0; x < 128; x += 1) {
                int hex = 0xFFFFFFFF & HSBtoRGB((float) x / 127.0f, (float) y / 127.0f, (float) y / 127.0f);
                defaultImage.setRGB(x, y, hex);
            }
        }
    }

    @Override
    public void onDisable() {
        Gui.disable();
        Photo.setPhotoDataGetter(null);
        Photo.setPhotoIdGetter(null);
    }

    /**
     * Download an image with the given URL and return an informative
     * DownloadResult.
     * The image will be scaled to 128x128 pixels if necessary.
     */
    protected DownloadResult downloadImage(URL url, boolean adminOverride) {
        try {
            URLConnection urlConnection = url.openConnection();
            int contentLength = urlConnection.getContentLength();
            if (contentLength < 0) contentLength = maxFileSize;
            if (!adminOverride && contentLength > maxFileSize) {
                return DownloadStatus.TOO_LARGE.make();
            }
            InputStream in = urlConnection.getInputStream();
            byte[] buf = new byte[contentLength];
            int r = -1;
            for (int total = 0; total < contentLength;) {
                r = in.read(buf, total, contentLength - total);
                if (r == 0) return DownloadStatus.NOT_FOUND.make();
                if (r == -1) break;
                total += r;
            }
            in.close();
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(buf));
            if (image == null) return DownloadStatus.NOT_IMAGE.make();
            // Crop
            if (image.getWidth() > image.getHeight()) {
                BufferedImage cropped = new BufferedImage(image.getHeight(), image.getHeight(), TYPE_INT_ARGB);
                Graphics2D gfx = cropped.createGraphics();
                gfx.drawImage(image, (image.getWidth() - image.getHeight()) / -2, 0, null);
                gfx.dispose();
                image = cropped;
            } else if (image.getHeight() > image.getWidth()) {
                BufferedImage cropped = new BufferedImage(image.getWidth(), image.getWidth(), TYPE_INT_ARGB);
                Graphics2D gfx = cropped.createGraphics();
                gfx.drawImage(image, 0, (image.getHeight() - image.getWidth()) / -2, null);
                gfx.dispose();
                image = cropped;
            }
            if (image.getWidth() != 128 || image.getHeight() != 128) {
                image = toBufferedImage(image.getScaledInstance(128, 128, Image.SCALE_SMOOTH));
            }
            return DownloadStatus.SUCCESS.make(image);
        } catch (IOException ioe) {
            return DownloadStatus.NOT_IMAGE.make(ioe);
        }
    }

    private static BufferedImage toBufferedImage(Image image) {
        if (image instanceof BufferedImage) return (BufferedImage) image;
        BufferedImage result = new BufferedImage(128, 128, TYPE_INT_ARGB);
        Graphics2D gfx = result.createGraphics();
        gfx.drawImage(image, 0, 0, null);
        gfx.dispose();
        return result;
    }

    /**
     * Load a single image from a file with path
     * `plugins/Photos/photos/$path`.  The path is expected to come
     * from Photo#filename.
     */
    protected BufferedImage loadImage(PhotoRuntime photo) {
        try {
            File file = new File(imageFolder, photo.getRow().filename());
            if (!file.isFile() && !file.canRead()) return null;
            return ImageIO.read(file);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return null;
        }
    }

    protected void loadImageAsync(PhotoRuntime photo, Consumer<BufferedImage> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                BufferedImage image = loadImage(photo);
                Bukkit.getScheduler().runTask(this, () -> callback.accept(image));
            });
    }

    protected void downloadPhotoAsync(final PhotoRuntime photo,
                                      final URL url,
                                      final boolean adminOverride,
                                      final Consumer<DownloadResult> callback) {
        final File file = new File(imageFolder, photo.getRow().filename());
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                DownloadResult result = downloadImage(url, adminOverride);
                if (result.status() == DownloadStatus.SUCCESS) {
                    try {
                        ImageIO.write(result.image(), "png", file);
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                        result = DownloadStatus.NOSAVE.make(ioe);
                    }
                }
                final DownloadResult finalResult = result;
                Bukkit.getScheduler().runTask(this, () -> {
                        callback.accept(finalResult);
                    });
            });
    }

    private PhotoData getMytemsPhotoData(int photoId) {
        PhotoRuntime photo = photos.ofPhotoId(photoId);
        if (photo == null || !photo.isReady()) return null;
        return new PhotoData(photo.getRow().getOwner(),
                             photo.getMapView().getId(),
                             photo.getRow().getColor(),
                             photo.getRow().getName());
    }

    private int mapIdToPhotoId(int mapId) {
        PhotoRuntime photo = photos.ofMapId(mapId);
        if (photo == null) return -1;
        return photo.getPhotoId();
    }
}
