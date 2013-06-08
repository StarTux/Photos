package com.winthier.photos;

import java.awt.image.BufferedImage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.map.MapPalette;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

public class Photo {
        private final short mapId;
        private BufferedImage image;
        public boolean dirty, loaded;

        public Photo(short mapId) {
                this.mapId = mapId;
        }

        public short getMapId() {
                return mapId;
        }

        public MapView getMapView() {
                return Bukkit.getServer().getMap(mapId);
        }

        public BufferedImage getImage() {
                return image;
        }

        public String getOwnerName() {
                return PhotosPlugin.getInstance().photosConfig.getMapOwner(mapId);
        }

        public boolean setOwner(Player player) {
                return PhotosPlugin.getInstance().photosConfig.setMapOwner(mapId, player);
        }

        public String getName() {
                return PhotosPlugin.getInstance().photosConfig.getMapName(mapId);
        }

        public void setName(String name) {
                PhotosPlugin.getInstance().photosConfig.setMapName(mapId, name);
        }

        public void clearImage() {
                image = null;
        }

        public ItemStack createItem(String name) {
                ItemStack result = new ItemStack(Material.MAP, 1, mapId);
                ItemMeta meta = result.getItemMeta();
                meta.setDisplayName(name);
                result.setItemMeta(meta);
                return result;
        }

        public String getFilename() {
                return String.format("%05d.png", mapId);
        }

        public synchronized void load() {
                image = Util.loadImageFromFile(getFilename());
                dirty = true;
        }

        public synchronized boolean save() {
                return Util.saveImageToFile(image, getFilename());
        }

        public synchronized boolean loadURL(final String url) {
                BufferedImage img = Util.loadImageFromURL(url);
                if (img == null) return false;
                image = MapPalette.resizeImage(img);
                dirty = true;
                return true;
        }

        public void initialize() {
                MapView view = getMapView();
                if (view == null) return;
                view.setCenterX(0x7fffffff);
                view.setCenterZ(0x7fffffff);
                view.setScale(MapView.Scale.FARTHEST);
                for (MapRenderer renderer : view.getRenderers()) {
                        view.removeRenderer(renderer);
                }
                view.addRenderer(new PhotoRenderer(this));
        }
}
