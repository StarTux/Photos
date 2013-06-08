package com.winthier.photos;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;

public class Util {
        public static BufferedImage loadImageFromURL(String string) {
                try {
                        URL url = new URL(string);
                        return ImageIO.read(url);
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
}
