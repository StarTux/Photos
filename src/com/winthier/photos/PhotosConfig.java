package com.winthier.photos;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

/**
 * Represent the photos file storage
 */
public class PhotosConfig {
        private final PhotosPlugin plugin;
        private YamlConfiguration config;
        private final List<Entry> list = new ArrayList<Entry>();
        private final Map<Short, Entry> idMap = new TreeMap<Short, Entry>();
        private final Map<String, List<Entry>> ownerMap = new HashMap<String, List<Entry>>();

        public PhotosConfig(PhotosPlugin plugin) {
                this.plugin = plugin;
        }

        private File getSaveFile() {
                return new File(plugin.getDataFolder(), "photos.yml");
        }

        private void load() {
                YamlConfiguration def = YamlConfiguration.loadConfiguration(plugin.getResource("photos.yml"));
                config = YamlConfiguration.loadConfiguration(getSaveFile());
                config.setDefaults(def);
                config.options().copyDefaults(true);
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
                                        ownerEntries = new LinkedList<Entry>();
                                        ownerMap.put(entry.getOwner(), ownerEntries);
                                }
                                ownerEntries.add(entry);
                        }
                }
        }

        private void save() {
                try {
                        config.set("photos", list);
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
                return getPlayerMaps(player.getName());
        }

        public List<Short> getPlayerMaps(String player) {
                List<Entry> entries = ownerMap.get(player.toLowerCase());
                if (entries == null) return Collections.<Short>emptyList();
                List<Short> result = new ArrayList<Short>(entries.size());
                for (Entry e : entries) result.add(e.getId());
                return result;
        }

        public boolean setMapOwner(short mapId, Player player) {
                return setMapOwner(mapId, player.getName());
        }

        public boolean setMapOwner(short mapId, String player) {
                Entry e = idMap.get(mapId);
                if (e == null || e.getOwner() != null) return false;
                e.setOwner(player.toLowerCase());
                List<Entry> es = ownerMap.get(player.toLowerCase());
                if (es == null) {
                        es = new LinkedList<Entry>();
                        ownerMap.put(player.toLowerCase(), es);
                }
                es.add(e);
                return true;
        }

        public boolean hasPlayerMap(Player player, short mapId) {
                return hasPlayerMap(player.getName(), mapId);
        }

        public boolean hasPlayerMap(String player, short mapId) {
                Entry e = idMap.get(mapId);
                if (e == null) return false;
                return player.equalsIgnoreCase(e.getOwner());
        }

        public String getMapOwner(short mapId) {
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
                return getPlayerBlanks(player.getName());
        }

        public int getPlayerBlanks(String player) {
                return getConfig().getInt("blanks." + player.toLowerCase());
        }

        public void setPlayerBlanks(Player player, int blanks) {
                setPlayerBlanks(player.getName(), blanks);
        }

        public void setPlayerBlanks(String player, int blanks) {
                getConfig().set("blanks." + player.toLowerCase(), blanks);
        }

        public void addPlayerBlanks(Player player, int blanks) {
                addPlayerBlanks(player.getName(), blanks);
        }

        public void addPlayerBlanks(String player, int blanks) {
                blanks += getPlayerBlanks(player);
                setPlayerBlanks(player, blanks);
        }
}

class Entry extends HashMap<String, Object> {
        private final short id;
        private String owner;
        private String name;

        public Entry(short id, String owner, String name) {
                this.id = id;
                this.owner = owner;
                this.name = name;
                initMap();
        }

        public Entry(Map<?, ?> map) {
                id = ((Number)map.get("id")).shortValue();
                owner = (String)map.get("owner");
                name = (String)map.get("name");
                initMap();
        }

        private void initMap() {
                put("id", id);
                if (owner != null) put("owner", owner);
                else remove("owner");
                if (name != null) put("name", name);
                else remove("name");
        }

        public short getId() {
                return id;
        }

        public String getOwner() {
                return owner;
        }

        public String getName() {
                return name;
        }

        public void setOwner(String owner) {
                this.owner = owner;
                if (owner == null) remove("owner");
                else put("owner", owner);
        }

        public void setName(String name) {
                this.name = name;
                if (name == null) remove("name");
                else put("name", name);
        }
}
