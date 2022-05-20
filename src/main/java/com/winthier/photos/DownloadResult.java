package com.winthier.photos;

import java.awt.image.BufferedImage;

/**
 * Simple container for DownloadStatus, BufferedImage, and
 * Exception.  The status may not be null.
 * @param status the status
 * @param image the image if any, else null
 * @param exception the exception if any, else null
 */
public record DownloadResult(DownloadStatus status,
                             BufferedImage image,
                             Exception exception) {
}
