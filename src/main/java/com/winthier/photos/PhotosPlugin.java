package com.winthier.photos;

import com.winthier.generic_events.GenericEvents;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import lombok.Getter;
import lombok.Value;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * The main plugin class.
 * An instance of this manages all Photos and configuration values,
 * and runs some utility functions to find or modify Photos, and
 * perform Input/Output via disk or network.
 */
@Getter
public final class PhotosPlugin extends JavaPlugin {
    private double photoPrice;
    private double copyPrice;
    private long loadCooldown;
    private int maxFileSize;
    private List<Photo> photos;
    private PhotosDatabase database;
    private PhotoCommand photoCommand;
    private AdminCommand adminCommand;
    private String defaultDownloadURL;
    private BufferedImage defaultImage;
    private String rules;

    // --- JavaPlugin

    @Override
    public void onEnable() {
        database = new PhotosDatabase(this);
        if (!database.createTables()) {
            getLogger().warning("Database error. Disabling plugin.");
            setEnabled(false);
            return;
        }
        saveDefaultConfig();
        saveResource("default.png", false);
        importConfig();
        loadPhotos();
        photoCommand = new PhotoCommand(this);
        adminCommand = new AdminCommand(this);
        getCommand("photo").setExecutor(photoCommand);
        getCommand("photoadmin").setExecutor(adminCommand);
        getServer().getPluginManager().registerEvents(new InventoryListener(this), this);
        getServer().getScheduler().runTaskTimer(this, PhotoRenderer::onTick, 1L, 1L);
    }

    @Override
    public void onDisable() {
        for (Player player: getServer().getOnlinePlayers()) {
            // Close all open PhotoMenus
            InventoryView openView = player.getOpenInventory();
            if (openView == null) continue;
            if (openView.getTopInventory().getHolder() instanceof PhotosMenu) {
                player.closeInventory();
            }
            // Remove all metadata in case we placed them.
            player.removeMetadata(PhotoCommand.META_COOLDOWN, this);
        }
    }

    // --- Configuration

    /**
     * Reload and import the config.yml.
     */
    void importConfig() {
        // load config.yml
        reloadConfig();
        photoPrice = getConfig().getDouble("PhotoPrice");
        copyPrice = getConfig().getDouble("CopyPrice");
        loadCooldown = getConfig().getLong("LoadCooldown");
        maxFileSize = getConfig().getInt("MaxFileSize") * 1024;
        defaultDownloadURL = getConfig().getString("DefaultDownloadURL");
        String text = getConfig().getString("Rules");
        rules = text != null
            ? ChatColor.translateAlternateColorCodes('&', text)
            : "No rules specified";
        // load default.png
        try {
            defaultImage = ImageIO.read(new File(getDataFolder(), "default.png"));
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
     * Load all existing photos from the database and initialize
     * Minecraft maps so they can display them properly via
     * PhotoRenderer.
     */
    boolean loadPhotos() {
        photos = database.loadPhotos();
        if (photos == null) {
            getLogger().warning("Could not load photos.");
            return false;
        }
        // Initialize maps
        for (Photo photo: photos) {
            MapView view = getServer().getMap((short) photo.getId());
            if (view == null) {
                getLogger().warning("Map id " + photo.getId()
                                    + " does not exist but has a photo in storage.");
                continue;
            }
            initializeMapView(view, photo);
        }
        return true;
    }

    // --- Photos

    /**
     * Find Photo with given id.
     */
    Photo findPhoto(int id) {
        for (Photo photo: photos) {
            if (photo.getId() == id) return photo;
        }
        return null;
    }

    /**
     * Find Photo for given map item.  The item must be a FILLED_MAP.
     */
    Photo findPhoto(ItemStack item) {
        MapMeta meta = (MapMeta) item.getItemMeta();
        int mapId = (int) meta.getMapId();
        return findPhoto(mapId);
    }

    /**
     * Find Photos belonging to player with given unique id.
     */
    List<Photo> findPhotos(UUID playerId) {
        List<Photo> result = new ArrayList<>();
        for (Photo photo: photos) {
            if (playerId.equals(photo.getOwner())) result.add(photo);
        }
        return result;
    }

    /**
     * Create a new Photo for the given owner, with the given name and
     * color.
     */
    Photo createPhoto(UUID owner, String name, int color) {
        MapView view = getServer().createMap(getServer().getWorlds().get(0));
        if (view == null) return null;
        return createPhoto(view, owner, name, color);
    }

    /**
     * Same as above, but use an existing map id.
     */
    Photo createPhoto(int id, UUID owner, String name, int color) {
        if (findPhoto(id) != null) return null;
        MapView view = getServer().getMap((short) id);
        if (view == null) return null;
        return createPhoto(view, owner, name, color);
    }

    private Photo createPhoto(MapView view, UUID owner, String name, int color) {
        Photo photo = new Photo();
        int mapId = (int) view.getId();
        photo.setId(mapId);
        photo.setOwner(owner);
        photo.setName(name);
        photo.setColor(color);
        if (!database.savePhoto(photo)) {
            getLogger().warning("Failed to save photo with map id " + mapId);
        }
        initializeMapView(view, photo);
        photos.add(photo);
        return photo;
    }

    /**
     * Update a Map item to corresponds with the given Photo.
     */
    void updatePhotoItem(ItemStack item, Photo photo) {
        MapMeta meta = (MapMeta) item.getItemMeta();
        meta.setMapId(photo.getId());
        meta.setScaling(false);
        meta.setColor(Color.fromRGB(photo.getColor()));
        meta.setLocationName(photo.getName());
        meta.setLocalizedName("Photo");
        List<String> lore = new ArrayList<>();
        lore.add("" + ChatColor.LIGHT_PURPLE + ChatColor.ITALIC + "Photo");
        if (photo.getName() != null) {
            lore.add(ChatColor.GRAY + "Name " + ChatColor.RESET + photo.getName());
        }
        if (photo.getOwner() != null) {
            String name = GenericEvents.cachedPlayerName(photo.getOwner());
            if (name == null) name = "";
            lore.add(ChatColor.GRAY + "Owner " + ChatColor.RESET + name);
        }
        lore.add(ChatColor.GRAY + "See " + ChatColor.RESET + "/photo");
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    /**
     * Spawn a new Map item which corresponds with the given Photo.
     */
    ItemStack createPhotoItem(Photo photo) {
        ItemStack result = new ItemStack(Material.FILLED_MAP);
        updatePhotoItem(result, photo);
        return result;
    }

    /**
     * Set up a single map view so it can display the given Photo.
     */
    public void initializeMapView(MapView view, Photo photo) {
        if (view == null) return;
        view.setCenterX(0x7fffffff);
        view.setCenterZ(0x7fffffff);
        view.setScale(MapView.Scale.FARTHEST);
        for (MapRenderer renderer : new ArrayList<>(view.getRenderers())) {
            view.removeRenderer(renderer);
        }
        view.addRenderer(new PhotoRenderer(this, photo));
    }

    /**
     * Delete a photo from memory cache and database.
     */
    public boolean deletePhoto(Photo photo) {
        photos.remove(photo);
        return database.deletePhoto(photo.getId());
    }

    // --- Photo I/O

    /**
     * Simple enum to be contained in DownloadResult (see below) to
     * inform clients of downloadImage() or downloadImage() about
     * the result of the operation.
     */
    enum DownloadStatus {
        SUCCESS,
        NOT_FOUND,
        TOO_LARGE,
        NOT_IMAGE,
        NOSAVE,
        UNKNOWN;
        DownloadResult make(BufferedImage image) {
            return new DownloadResult(this, image, null);
        }
        DownloadResult make() {
            return new DownloadResult(this, null, null);
        }
        DownloadResult make(Exception exception) {
            return new DownloadResult(this, null, exception);
        }
    }

    /**
     * Simple container for DownloadStatus, BufferedImage, and
     * Exception.  The status may not be null.
     */
    @Value
    static final class DownloadResult {
        public final DownloadStatus status;
        public final BufferedImage image;
        public final Exception exception;
    }

    /**
     * Download an image with the given URL and return an informative
     * DownloadResult.
     * The image will be scaled to 128x128 pixels if necessary.
     */
    DownloadResult downloadImage(URL url, boolean adminOverride) {
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
                BufferedImage cropped = new BufferedImage(image.getHeight(), image.getHeight(),
                                                          BufferedImage.TYPE_INT_ARGB);
                Graphics2D gfx = cropped.createGraphics();
                gfx.drawImage(image, (image.getWidth() - image.getHeight()) / -2, 0, null);
                gfx.dispose();
                image = cropped;
            } else if (image.getHeight() > image.getWidth()) {
                BufferedImage cropped = new BufferedImage(image.getWidth(), image.getWidth(),
                                                          BufferedImage.TYPE_INT_ARGB);
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
        BufferedImage result = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
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
    BufferedImage loadImage(String path) {
        try {
            File dir = new File(getDataFolder(), "photos");
            dir.mkdirs();
            File file = new File(dir, path);
            if (!file.isFile() && !file.canRead()) return null;
            return ImageIO.read(file);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return null;
        }
    }

    void loadImageAsync(String path, Consumer<BufferedImage> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                BufferedImage image = loadImage(path);
                Bukkit.getScheduler().runTask(this, () -> callback.accept(image));
            });
    }

    void downloadPhotoAsync(final Photo photo, final URL url, final boolean adminOverride,
                            final Consumer<DownloadResult> callback) {
        final File dir = new File(getDataFolder(), "photos");
        final File file = new File(dir, photo.filename());
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                DownloadResult result = downloadImage(url, adminOverride);
                if (result.status == DownloadStatus.SUCCESS) {
                    try {
                        dir.mkdirs();
                        ImageIO.write(result.image, "png", file);
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                        result = DownloadStatus.NOSAVE.make();
                    }
                }
                final DownloadResult finalResult = result;
                Bukkit.getScheduler().runTask(this, () -> {
                        if (finalResult.status == DownloadStatus.SUCCESS) {
                            MapView view = getServer().getMap((short) photo.getId());
                            if (view != null) initializeMapView(view, photo);
                            callback.accept(finalResult);
                        } else {
                            callback.accept(finalResult);
                        }
                    });
            });
    }
}
