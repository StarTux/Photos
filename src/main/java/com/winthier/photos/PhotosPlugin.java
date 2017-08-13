package com.winthier.photos;

import java.util.Map;
import java.util.TreeMap;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MapView;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class PhotosPlugin extends JavaPlugin {
    private static PhotosPlugin instance;
    private final Map<Short, Photo> photos = new TreeMap<Short, Photo>();
    public final PhotosConfig photosConfig = new PhotosConfig(this);
    public final PhotosMenu photosMenu = new PhotosMenu(this);
    public Economy economy;
    public double mapPrice, blankPrice;
    public long loadCooldown;
    public int maxFileSize;

    @Override
    public void onEnable() {
        instance = this;
        reloadConfig();
        saveDefaultConfig();
        load();
        getCommand("photo").setExecutor(new PhotoCommand(this));
        if (!setupEconomy()) {
            getLogger().warning("Economy setup failed!");
        } else {
            photosMenu.onEnable();
        }
    }

    @Override
    public void onDisable() {
        photosMenu.onDisable();
        instance = null;
    }

    public static PhotosPlugin getInstance() {
        return instance;
    }

    public void load() {
        // load config.yml
        reloadConfig();
        mapPrice = getConfig().getDouble("MapPrice");
        blankPrice = getConfig().getDouble("BlankPrice");
        loadCooldown = getConfig().getLong("LoadCooldown");
        maxFileSize = getConfig().getInt("MaxFileSize") * 1024;
        // load photos.yml
        photos.clear();
        photosConfig.reloadConfig();
        for (short mapId : photosConfig.getMaps()) {
            final Photo photo = createPhoto(mapId);
        }
    }

    public void save() {
        saveConfig();
        photosConfig.saveConfig();
    }

    public Photo getPhoto(short mapId) {
        Photo photo = photos.get(mapId);
        return photo;
    }

    private Photo createPhoto(short mapId) {
        Photo photo = new Photo(mapId);
        photos.put(mapId, photo);
        photo.initialize();
        return photo;
    }

    public Photo createPhoto() {
        MapView mapView = getServer().createMap(getServer().getWorlds().get(0));
        if (!photosConfig.addMap(mapView.getId())) return null;
        return createPhoto(mapView.getId());
    }

    public void createPhoto(Player player, String name) {
        int blanks = photosConfig.getPlayerBlanks(player);
        if (blanks < 1) {
            player.sendMessage("" + ChatColor.RED + "You do not have any blank photos.");
            return;
        }
        Photo photo = createPhoto();
        if (photo == null) {
            player.sendMessage("" + ChatColor.RED + "Photo creation failed. Contact an administrator.");
            return;
        }
        photo.setOwner(player);
        photo.setName(name);
        photosConfig.setPlayerBlanks(player, blanks - 1);
        photosConfig.saveConfig();
        ItemStack item = photo.createItem("" + ChatColor.GOLD + name);
        player.getWorld().dropItem(player.getEyeLocation(), item);
        player.sendMessage("" + ChatColor.GREEN + "Photo created. To load an image, use the following command:");
        player.sendMessage("" + ChatColor.YELLOW + "/photo load <url>");
        getLogger().info(player.getName() + " created map #" + photo.getMapId());
    }

    private boolean setupEconomy()
    {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(Economy.class);
        if (economyProvider != null) {
            economy = economyProvider.getProvider();
        }
        return (economy != null);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String args[]) {
        return false;
    }
}
