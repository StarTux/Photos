package com.winthier.photos;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Color;
import static java.awt.Color.HSBtoRGB;

public enum PhotoColor {
    AQUA(Color.AQUA),
    BLACK(Color.BLACK),
    BLUE(Color.BLUE),
    FUCHSIA(Color.FUCHSIA),
    GRAY(Color.GRAY),
    GREEN(Color.GREEN),
    LIME(Color.LIME),
    MAROON(Color.MAROON),
    NAVY(Color.NAVY),
    OLIVE(Color.OLIVE),
    ORANGE(Color.ORANGE),
    PURPLE(Color.PURPLE),
    RED(Color.RED),
    SILVER(Color.SILVER),
    TEAL(Color.TEAL),
    WHITE(Color.WHITE),
    YELLOW(Color.YELLOW);

    public final Color color;

    PhotoColor(final Color color) {
        this.color = color;
    }

    public static PhotoColor random() {
        PhotoColor[] v = values();
        return v[ThreadLocalRandom.current().nextInt(v.length)];
    }

    public static int totallyRandom() {
        Random random = ThreadLocalRandom.current();
        float hue = random.nextFloat();
        return 0xFFFFFF & HSBtoRGB(hue, 0.66f, 1.0f);
    }
}
