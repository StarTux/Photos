package com.winthier.photos.sql;

import com.winthier.sql.SQLRow;
import java.util.Date;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;

@Data @Table(name = "consent")
public final class SQLConsent implements SQLRow {
    @Id
    private Integer id;

    @Column(nullable = true, unique = true)
    private UUID player;

    @Column(nullable = false)
    private Date accepted;

    public SQLConsent() { }

    public SQLConsent(final UUID player) {
        this.player = player;
        this.accepted = new Date();
    }
}
