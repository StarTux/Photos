package com.winthier.photos;

import java.util.UUID;
import lombok.Data;

/**
 * Simple serialization type.
 */
@Data
final class Photo {
    private int id = -1;
    private UUID owner = null;
    private String name = "";
    private int color = 0;

    String filename() {
        return String.format("%05d.png", id);
    }
}
