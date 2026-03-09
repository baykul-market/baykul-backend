package by.baykulbackend.database.dao.user;

import by.baykulbackend.database.dto.security.Views;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Objects;
import java.util.UUID;

@Data
@Entity
@Table(name = "refresh_token")
@Schema(description = "Refresh token entity for JWT authentication session management")
public class RefreshToken {
    @Schema(
            description = "Unique identifier",
            accessMode = Schema.AccessMode.READ_ONLY,
            example = "123e4567-e89b-12d3-a456-426614174000"
    )
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @JsonView({Views.RefreshTokenView.Get.class, Views.RefreshTokenView.Patch.class})
    private UUID id;

    @Schema(
            description = "JWT refresh token string",
            accessMode = Schema.AccessMode.READ_ONLY,
            example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJqb2huX2RvZSIsImlhdC..."
    )
    @Column(name = "name")
    @JsonView({Views.RefreshTokenView.Get.class})
    private String name;

    @Schema(
            description = "User agent string of the client device/browser",
            nullable = true,
            example = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    )
    @Column(name = "user_agent")
    @JsonView({Views.RefreshTokenView.Get.class, Views.RefreshTokenView.Patch.class})
    public String userAgent;

    @Schema(
            description = "IP address of the client",
            nullable = true,
            example = "192.168.1.100"
    )
    @Column(name = "ip_address")
    @JsonView({Views.RefreshTokenView.Get.class, Views.RefreshTokenView.Patch.class})
    public String ipAddress;

    @Schema(
            description = "User associated with this refresh token",
            accessMode = Schema.AccessMode.READ_ONLY,
            example = "{\"id\": \"123e4567-e89b-12d3-a456-426614174001\", \"login\": \"john_doe\"}"
    )
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonView(Views.RefreshTokenFullView.class)
    private User user;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        RefreshToken refreshToken = (RefreshToken) o;

        return Objects.equals(id, refreshToken.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}