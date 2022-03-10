package com.winthier.photos;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class PhotosDatabase {
    private final PhotosPlugin plugin;
    private Connection cachedConnection = null;

    PhotosDatabase(final PhotosPlugin plugin) {
        this.plugin = plugin;
    }

    Connection getConnection() throws SQLException {
        if (cachedConnection == null || !cachedConnection.isValid(1)) {
            try {
                Class.forName("org.sqlite.JDBC");
            } catch (ClassNotFoundException cnfe) {
                cnfe.printStackTrace();
                return null;
            }
            File dbfolder = plugin.getDataFolder();
            dbfolder.mkdirs();
            File dbfile = new File(dbfolder, "photos.db");
            cachedConnection = DriverManager.getConnection("jdbc:sqlite:" + dbfile);
        }
        return cachedConnection;
    }

    boolean createTables() {
        String sql;
        sql =
            "CREATE TABLE IF NOT EXISTS `photos` ("
            + " `id` INTEGER PRIMARY KEY,"
            + " `owner` VARCHAR(40) DEFAULT NULL,"
            + " `name` VARCHAR(255) NOT NULL DEFAULT 'New Photo',"
            + " `color` INTEGER NOT NULL DEFAULT 0,"
            + " `extra` VARCHAR(255) NOT NULL DEFAULT '{}'" // For future use
            + ")";
        try {
            getConnection().createStatement().execute(sql);
        } catch (SQLException sqle) {
            sqle.printStackTrace();
            return false;
        }
        sql =
            "CREATE TABLE IF NOT EXISTS `consent` ("
            //+ " `id` INTEGER PRIMARY KEY,"
            + " `uuid` VARCHAR(40) NOT NULL PRIMARY KEY"
            + ")";
        try {
            getConnection().createStatement().execute(sql);
        } catch (SQLException sqle) {
            sqle.printStackTrace();
            return false;
        }
        return true;
    }

    List<Photo> loadPhotos() {
        List<Photo> result = new ArrayList<>();
        String sql = "SELECT `id`, `owner`, `name`, `color`, `extra` FROM `photos`";
        try (ResultSet row = getConnection().createStatement().executeQuery(sql)) {
            while (row.next()) {
                Photo photo = new Photo();
                photo.setId(row.getInt("id"));
                String val = row.getString("owner");
                if (val != null) {
                    try {
                        photo.setOwner(UUID.fromString(val));
                    } catch (IllegalArgumentException iae) {
                        plugin.getLogger().warning("Invalid owner id: " + val);
                        iae.printStackTrace();
                    }
                }
                photo.setName(row.getString("name"));
                photo.setColor(row.getInt("color"));
                result.add(photo);
            }
            return result;
        } catch (SQLException sqle) {
            sqle.printStackTrace();
            return null;
        }
    }

    boolean savePhoto(Photo photo) {
        String sql = "INSERT INTO `photos` (`id`, `owner`, `name`, `color`) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setInt(1, photo.getId());
            if (photo.getOwner() == null) {
                statement.setString(2, null);
            } else {
                statement.setString(2, photo.getOwner().toString());
            }
            statement.setString(3, photo.getName());
            statement.setInt(4, photo.getColor());
            return 1 == statement.executeUpdate();
        } catch (SQLException sqle) {
            sqle.printStackTrace();
            return false;
        }
    }

    boolean updatePhoto(Photo photo) {
        String sql = "UPDATE `photos` SET `owner`=?, `name`=?, `color`=? WHERE `id`=?";
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            if (photo.getOwner() == null) {
                statement.setString(1, null);
            } else {
                statement.setString(1, photo.getOwner().toString());
            }
            statement.setString(2, photo.getName());
            statement.setInt(3, photo.getColor());
            statement.setInt(4, photo.getId());
            return 1 == statement.executeUpdate();
        } catch (SQLException sqle) {
            sqle.printStackTrace();
            return false;
        }
    }

    boolean deletePhoto(int id) {
        String sql = "DELETE FROM `photos` WHERE `id`=?";
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setInt(1, id);
            return 1 == statement.executeUpdate();
        } catch (SQLException sqle) {
            sqle.printStackTrace();
            return false;
        }
    }

    boolean didConsent(UUID uuid) {
        String sql = "SELECT * FROM `consent` WHERE `uuid` = ?";
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        } catch (SQLException sqle) {
            sqle.printStackTrace();
            return false;
        }
    }

    boolean consent(UUID uuid) {
        String sql = "INSERT OR IGNORE INTO `consent` (`uuid`) VALUES (?)";
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            return 1 == statement.executeUpdate();
        } catch (SQLException sqle) {
            sqle.printStackTrace();
            return false;
        }
    }

    boolean resetConsent() {
        String sql = "DELETE FROM `consent`";
        try {
            getConnection().createStatement().execute(sql);
            return true;
        } catch (SQLException sqle) {
            sqle.printStackTrace();
            return false;
        }
    }

    protected int transfer(UUID from, UUID to) {
        String sql = "UPDATE `photos` SET `owner`=? WHERE `owner`=?";
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setString(1, to.toString());
            statement.setString(2, from.toString());
            return statement.executeUpdate();
        } catch (SQLException sqle) {
            sqle.printStackTrace();
            return 0;
        }
    }
}
