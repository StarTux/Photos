package com.winthier.photos;

import java.awt.image.BufferedImage;

/**
 * Simple enum to be contained in DownloadResult (see below) to
 * inform clients of downloadImage() or downloadImage() about
 * the result of the operation.
 */
public enum DownloadStatus {
    SUCCESS,
    NOT_FOUND,
    TOO_LARGE,
    NOT_IMAGE,
    NOSAVE,
    UNKNOWN;

    public DownloadResult make(BufferedImage image) {
        return new DownloadResult(this, image, null);
    }

    public DownloadResult make() {
        return new DownloadResult(this, null, null);
    }

    public DownloadResult make(Exception exception) {
        return new DownloadResult(this, null, exception);
    }

    public boolean isSuccessful() {
        return this == SUCCESS;
    }
}
