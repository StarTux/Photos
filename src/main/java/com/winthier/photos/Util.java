package com.winthier.photos;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import javax.imageio.ImageIO;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class Util {
        public static BufferedImage loadImageFromURL(String string, Player player) {
                try {
                        final int MAX_FILE_SIZE = PhotosPlugin.getInstance().maxFileSize;
                        URL url = new URL(string);
                        URLConnection urlConnection = url.openConnection();
                        if (urlConnection.getContentLength() > MAX_FILE_SIZE) {
                                sendAsyncMessage(player, "" + ChatColor.RED + "File too long");
                                return null;
                        }
                        InputStream in = urlConnection.getInputStream();
                        byte buf[] = new byte[MAX_FILE_SIZE];
                        int r = 0;
                        for (int total = 0; total < MAX_FILE_SIZE; ) {
                                if (0 > (r = in.read(buf, total, MAX_FILE_SIZE - total))) break;
                                total += r;
                        }
                        if (r != -1) {
                                sendAsyncMessage(player, "" + ChatColor.RED + "File too long");
                                return null;
                        }
                        return ImageIO.read(new ByteArrayInputStream(buf));
                } catch (IOException e) {
                        return null;
                }
        }

        public static BufferedImage loadImageFromFile(String path) {
                try {
                        File file = new File(PhotosPlugin.getInstance().getDataFolder(), "photos");
                        file.mkdirs();
                        return ImageIO.read(new File(file, path));
                } catch (IOException e) {
                        return null;
                }
        }

        public static boolean saveImageToFile(BufferedImage image, String path) {
                try {
                        File file = new File(PhotosPlugin.getInstance().getDataFolder(), "photos");
                        file.mkdirs();
                        ImageIO.write(image, "png", new File(file, path));
                        return true;
                } catch (IOException e) {
                        return false;
                }
        }

        public static void sendAsyncMessage(final Player sender, final String message) {
                new BukkitRunnable() {
                        public void run() {
                                sender.sendMessage(message);
                        }
                }.runTask(PhotosPlugin.getInstance());
        }
}
