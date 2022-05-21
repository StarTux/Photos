package com.winthier.photos.sql;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import lombok.Data;

@Data @Table(name = "photos",
             indexes = {
                 @Index(name = "owner", columnList = "owner"),
                 @Index(name = "updated", columnList = "updated")
             })
public final class SQLPhoto {
    @Id
    private Integer id;

    @Column(nullable = true)
    private UUID owner; // null = admin

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int color;

    @Column(nullable = false)
    private Date created;

    @Column(nullable = false)
    private Date updated;

    public SQLPhoto() { }

    public SQLPhoto(final UUID owner, final String name, final int color) {
        this.owner = owner;
        this.name = name;
        this.color = color;
        this.created = new Date();
        this.updated = created;
    }

    public String filename() {
        return String.format("%05d.png", id);
    }

    public int getHexColor() {
        return color & 0xFFFFFF;
    }

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yy/MM/dd");

    public String formatCreationDate() {
        return DATE_FORMAT.format(created);
    }
}
