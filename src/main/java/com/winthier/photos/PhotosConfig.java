package com.winthier.photos;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

/**
 * Represent the photos file storage
 */
public class PhotosConfig {
    private final PhotosPlugin plugin;
    private YamlConfiguration config;
    private final List<Entry> list = new ArrayList<>();
    private final Map<Short, Entry> idMap = new TreeMap<>();
    private final Map<UUID, List<Entry>> ownerMap = new HashMap<>();

    public PhotosConfig(PhotosPlugin plugin) {
        this.plugin = plugin;
    }

    private File getSaveFile() {
        return new File(plugin.getDataFolder(), "photos.yml");
    }

    private void load() {
        config = YamlConfiguration.loadConfiguration(getSaveFile());
        list.clear();
        idMap.clear();
        ownerMap.clear();
        for (Map<?, ?> m : config.getMapList("photos")) {
            Entry entry = new Entry(m);
            list.add(entry);
            idMap.put(entry.getId(), entry);
            if (entry.getOwner() != null) {
                List<Entry> ownerEntries = ownerMap.get(entry.getOwner());
                if (ownerEntries == null) {
                    ownerEntries = new ArrayList<Entry>();
                    ownerMap.put(entry.getOwner(), ownerEntries);
                }
                ownerEntries.add(entry);
            }
        }
    }

    private void save() {
        try {
            config.set("photos", list.stream().map(e -> e.serialize()).collect(Collectors.toList()));;
            config.save(getSaveFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void reloadConfig() {
        load();
    }

    public void saveConfig() {
        save();
    }

    public ConfigurationSection getConfig() {
        if (config == null) {
            load();
        }
        return config;
    }

    public Set<Short> getMaps() {
        return idMap.keySet();
    }

    public boolean hasMap(short mapId) {
        return idMap.get(mapId) != null;
    }

    public boolean addMap(short mapId) {
        if (idMap.get(mapId) != null) return false;
        Entry entry = new Entry(mapId, null, null);
        list.add(entry);
        idMap.put(mapId, entry);
        return true;
    }

    public List<Short> getPlayerMaps(Player player) {
        return getPlayerMaps(player.getUniqueId());
    }

    public List<Short> getPlayerMaps(UUID uuid) {
        List<Entry> entries = ownerMap.get(uuid);
        if (entries == null) return Collections.<Short>emptyList();
        List<Short> result = new ArrayList<Short>(entries.size());
        for (Entry e : entries) result.add(e.getId());
        return result;
    }

    public boolean setMapOwner(short mapId, Player player) {
        return setMapOwner(mapId, player.getUniqueId());
    }

    public boolean setMapOwner(short mapId, UUID uuid) {
        Entry e = idMap.get(mapId);
        if (e == null || e.getOwner() != null) return false;
        e.setOwner(uuid);
        List<Entry> es = ownerMap.get(uuid);
        if (es == null) {
            es = new ArrayList<>();
            ownerMap.put(uuid, es);
        }
        es.add(e);
        return true;
    }

    public boolean hasPlayerMap(Player player, short mapId) {
        return hasPlayerMap(player.getUniqueId(), mapId);
    }

    public boolean hasPlayerMap(UUID uuid, short mapId) {
        Entry e = idMap.get(mapId);
        if (e == null) return false;
        return uuid.equals(e.getOwner());
    }

    public UUID getMapOwner(short mapId) {
        Entry e = idMap.get(mapId);
        if (e == null) return null;
        return e.getOwner();
    }

    public String getMapName(short mapId) {
        Entry e = idMap.get(mapId);
        if (e == null) return null;
        return e.getName();
    }

    public void setMapName(short mapId, String name) {
        Entry e = idMap.get(mapId);
        if (e == null) return;
        e.setName(name);
    }

    public int getPlayerBlanks(Player player) {
        return getPlayerBlanks(player.getUniqueId());
    }

    public int getPlayerBlanks(UUID uuid) {
        return getConfig().getInt("blanks." + uuid.toString());
    }

    public void setPlayerBlanks(Player player, int blanks) {
        setPlayerBlanks(player.getUniqueId(), blanks);
    }

    public void setPlayerBlanks(UUID uuid, int blanks) {
        getConfig().set("blanks." + uuid.toString(), blanks);
    }

    public void addPlayerBlanks(Player player, int blanks) {
        addPlayerBlanks(player.getUniqueId(), blanks);
    }

    public void addPlayerBlanks(UUID uuid, int blanks) {
        blanks += getPlayerBlanks(uuid);
        setPlayerBlanks(uuid, blanks);
    }
}

@Getter @Setter
class Entry {
    private final short id;
    private UUID owner;
    private String name;

    public Entry(short id, UUID owner, String name) {
        this.id = id;
        this.owner = owner;
        this.name = name;
    }

    public Entry(Map<?, ?> map) {
        id = ((Number)map.get("id")).shortValue();
        owner = UUID.fromString((String)map.get("owner"));
        name = (String)map.get("name");
    }

    Map<String, Object> serialize() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", id);
        result.put("owner", owner.toString());
        result.put("name", name);
        return result;
    }
}
