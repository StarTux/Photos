package com.winthier.photos;

import com.winthier.photos.sql.SQLPhoto;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

/**
 * Photos index.
 */
@RequiredArgsConstructor
public final class Photos {
    private final PhotosPlugin plugin;
    private List<PhotoRuntime> all = new ArrayList<>();
    private Map<Integer, PhotoRuntime> photoIdMap = new TreeMap<>();
    private Map<Integer, PhotoRuntime> mapIdMap = new TreeMap<>();
    private File databaseFile;
    private Connection localConnection;
    private Date lastUpdate;

    protected void enable() {
        plugin.getDataFolder().mkdirs();
        databaseFile = new File(plugin.getDataFolder(), "local.db");
        load();
        Bukkit.getScheduler().runTaskTimer(plugin, this::update, 600L, 600L);
    }

    private void clear() {
        all.clear();
        photoIdMap.clear();
        mapIdMap.clear();
    }

    public PhotoRuntime ofPhotoId(int photoId) {
        return photoIdMap.get(photoId);
    }

    public PhotoRuntime ofMapId(int mapId) {
        return mapIdMap.get(mapId);
    }

    private void load() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException cnfe) {
            throw new IllegalStateException(cnfe);
        }
        try {
            localConnection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile);
            localConnection.createStatement()
                .execute("CREATE TABLE IF NOT EXISTS `photos` ("
                         + " `id` INTEGER PRIMARY KEY,"
                         + " `photo_id` INTEGER NOT NULL,"
                         + " `map_id` INTEGER NOT NULL,"
                         + " UNIQUE(`photo_id`),"
                         + " UNIQUE(`map_id`)"
                         + ")");
            ResultSet resultSet = localConnection.createStatement()
                .executeQuery("SELECT * FROM `photos`");
            while (resultSet.next()) {
                int photoId = resultSet.getInt("photo_id");
                int mapId = resultSet.getInt("map_id");
                put(photoId, mapId);
            }
        } catch (SQLException sqle) {
            throw new IllegalStateException(sqle);
        }
        lastUpdate = new Date();
        for (SQLPhoto row : plugin.getDatabase().find(SQLPhoto.class).findList()) {
            put(row);
        }
        for (PhotoRuntime photo : all) {
            if (!photo.isReady()) {
                plugin.getLogger().warning("Photo not ready:"
                                           + " photoId=" + photo.getPhotoId()
                                           + " mapId=" + photo.getMapId()
                                           + " row=" + (photo.getRow() != null)
                                           + " view=" + (photo.getMapView() != null)
                                           + " renderer=" + (photo.getRenderer() != null));
            }
        }
    }

    protected int pruneLocal() {
        List<Integer> ids = plugin.getDatabase().find(SQLPhoto.class)
            .findValues("id", Integer.class);
        int count = 0;
        for (PhotoRuntime it : all) {
            if (!ids.contains(it.getPhotoId())) {
                try {
                    localConnection.createStatement().execute("DELETE FROM `photos` WHERE `photo_id` = " +  it.getPhotoId());
                } catch (SQLException sqle) {
                    throw new IllegalStateException(sqle);
                }
                count += 1;
            }
        }
        return count;
    }

    public void update() {
        Date date = lastUpdate;
        lastUpdate = new Date();
        plugin.getDatabase().find(SQLPhoto.class)
            .gte("updated", date)
            .findListAsync(list -> {
                    for (SQLPhoto row : list) {
                        PhotoRuntime photo = photoIdMap.get(row.getId());
                        if (photo == null) {
                            photo = put(row);
                            plugin.getLogger().info("[Update] New photo:"
                                                    + " photoId=" + photo.getPhotoId()
                                                    + " mapId=" + photo.getMapId());
                        } else if (row.equals(photo.getRow())) {
                            continue;
                        } else {
                            photo.setRow(row);
                            if (photo.getRenderer() != null) {
                                photo.getRenderer().refresh();
                            }
                            plugin.getLogger().info("[Update] Photo changed:"
                                                    + " photoId=" + photo.getPhotoId()
                                                    + " mapId=" + photo.getMapId());
                        }
                    }
                });
    }

    /**
     * Store the database row.
     * Wart: This will create a MapView if none is present.  Usually
     * this method is expected to be called after all views have been
     * set up.  Should that ever change, consider refactoring the
     * MapView creation.
     */
    private PhotoRuntime put(SQLPhoto row) {
        PhotoRuntime photo = photoIdMap.get(row.getId());
        if (photo == null) {
            photo = new PhotoRuntime(row.getId());
            photoIdMap.put(row.getId(), photo);
            all.add(photo);
        }
        photo.setRow(row);
        if (photo.getMapView() != null) return photo;
        MapView mapView = Bukkit.createMap(Bukkit.getWorlds().get(0));
        final int mapId = mapView.getId();
        mapIdMap.put(mapId, photo);
        try {
            localConnection.createStatement()
                .executeUpdate("INSERT INTO `photos`"
                               + " (`photo_id`, `map_id`)"
                               + " VALUES"
                               + " (" + row.getId() + ", " + mapId + ")");
        } catch (SQLException sqle) {
            throw new IllegalStateException(sqle);
        }
        initializeMapView(mapView, photo);
        return photo;
    }

    /**
     * Save a mapping from photoId to mapId.  This is only used during
     * the initial load, before database rows are loaded.
     */
    private PhotoRuntime put(int photoId, int mapId) {
        PhotoRuntime photo = photoIdMap.get(photoId);
        if (photo == null) {
            photo = new PhotoRuntime(photoId);
            photoIdMap.put(photoId, photo);
            all.add(photo);
        }
        photo.setMapId(mapId);
        mapIdMap.put(mapId, photo);
        @SuppressWarnings("deprecation") MapView mapView = Bukkit.getMap((short) mapId);
        if (mapView == null) {
            plugin.getLogger().warning("MapView does not exist:"
                                       + " photoId=" + photo.getPhotoId()
                                       + " mapId=" + photo.getMapId());
            return photo;
        }
        initializeMapView(mapView, photo);
        return photo;
    }

    private void initializeMapView(MapView mapView, PhotoRuntime photo) {
        mapView.setCenterX(0x7fffffff);
        mapView.setCenterZ(0x7fffffff);
        mapView.setScale(MapView.Scale.FARTHEST);
        for (MapRenderer renderer : List.copyOf(mapView.getRenderers())) {
            mapView.removeRenderer(renderer);
        }
        PhotoRenderer renderer = new PhotoRenderer(plugin, photo);
        mapView.addRenderer(renderer);
        photo.setRenderer(renderer);
        photo.setMapView(mapView);
        photo.setMapId(mapView.getId());
    }

    public PhotoRuntime create(UUID owner, String name, int color) {
        SQLPhoto row = new SQLPhoto(owner, name, color);
        plugin.getDatabase().insert(row);
        PhotoRuntime photo = put(row);
        return photo;
    }

    public PhotoRuntime createLegacy(int mapId, UUID owner, String name, int color) {
        SQLPhoto row = new SQLPhoto(owner, name, color);
        plugin.getDatabase().insert(row);
        try {
            localConnection.createStatement()
                .executeUpdate("INSERT INTO `photos`"
                               + " (`photo_id`, `map_id`)"
                               + " VALUES"
                               + " (" + row.getId() + ", " + mapId + ")");
        } catch (SQLException sqle) {
            throw new IllegalStateException(sqle);
        }
        put(row.getId(), mapId);
        PhotoRuntime photo = put(row);
        return photo;
    }

    public List<PhotoRuntime> find(UUID owner) {
        List<PhotoRuntime> result = new ArrayList<>();
        for (PhotoRuntime photo : all) {
            if (!photo.isReady()) continue;
            if (Objects.equals(owner, photo.getRow().getOwner())) {
                result.add(photo);
            }
        }
        return result;
    }
}
