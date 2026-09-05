package by.baykulbackend.database.dao.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "part_sources")
public class PartSource {
    public static final UUID LEGACY_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final UUID MANUAL_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    public enum Status { ACTIVE, HIDDEN, ARCHIVED }

    @Id
    private UUID id;
    @Column(nullable = false)
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status = Status.HIDDEN;
    @Column(name = "system_source", nullable = false)
    private boolean systemSource;
    @Version
    private long version;
    @Column(name = "created_ts", nullable = false)
    private LocalDateTime createdTs = LocalDateTime.now();
    @Column(name = "updated_ts", nullable = false)
    private LocalDateTime updatedTs = LocalDateTime.now();
}
