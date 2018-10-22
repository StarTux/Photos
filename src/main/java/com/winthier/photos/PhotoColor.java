package com.winthier.photos;

import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Color;

enum PhotoColor {
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

    final Color color;

    PhotoColor(Color color) {
        this.color = color;
    }

    static PhotoColor random() {
        PhotoColor[] v = values();
        return v[ThreadLocalRandom.current().nextInt(v.length)];
    }
}
