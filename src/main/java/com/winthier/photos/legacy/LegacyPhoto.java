package com.winthier.photos.legacy;

import java.util.UUID;
import lombok.Data;

/**
 * Simple serialization type.
 */
@Data
public final class LegacyPhoto {
    private int id = -1;
    private UUID owner = null;
    private String name = "";
    private int color = 0;

    public String filename() {
        return String.format("%05d.png", id);
    }
}
