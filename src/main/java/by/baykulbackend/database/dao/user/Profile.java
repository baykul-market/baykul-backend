package by.baykulbackend.database.dao.user;

import by.baykulbackend.database.dto.security.Views;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Data
@Entity
@JsonIdentityInfo(
        generator = ObjectIdGenerators.PropertyGenerator.class,
        property = "id")
@Table(name = "profile")
@Schema(description = "User profile entity containing personal information")
public class Profile {
    @Schema(
            description = "Unique identifier",
            accessMode = Schema.AccessMode.READ_ONLY,
            example = "123e4567-e89b-12d3-a456-426614174000"
    )
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @JsonView(Views.UserView.Get.class)
    private UUID id;

    @Schema(
            description = "Timestamp when the profile was created",
            accessMode = Schema.AccessMode.READ_ONLY,
            example = "2024-01-15T10:30:00"
    )
    @Column(name = "created_ts")
    @JsonView(Views.UserView.Get.class)
    @CreationTimestamp
    private LocalDateTime createdTs;

    @Schema(
            description = "Timestamp when the profile was last updated",
            accessMode = Schema.AccessMode.READ_ONLY,
            example = "2024-01-20T14:45:30"
    )
    @Column(name = "updated_ts")
    @JsonView({Views.UserView.Get.class})
    @UpdateTimestamp
    private LocalDateTime updatedTs;

    @Schema(
            description = "User associated with this profile",
            accessMode = Schema.AccessMode.READ_ONLY,
            example = "{\"id\": \"123e4567-e89b-12d3-a456-426614174001\", \"login\": \"john_doe\"}"
    )
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Schema(
            description = "User's surname",
            maxLength = 50,
            nullable = true,
            example = "Doe"
    )
    @Column(name = "surname", length = 50)
    @JsonView({Views.UserView.Get.class, Views.UserView.Post.class, Views.UserView.Patch.class})
    private String surname;

    @Schema(
            description = "User's name",
            maxLength = 50,
            nullable = true,
            example = "John"
    )
    @Column(name = "name", length = 50)
    @JsonView({Views.UserView.Get.class, Views.UserView.Post.class, Views.UserView.Patch.class})
    private String name;

    @Schema(
            description = "User's patronymic",
            maxLength = 50,
            nullable = true,
            example = "Michael"
    )
    @Column(name = "patronymic", length = 50)
    @JsonView({Views.UserView.Get.class, Views.UserView.Post.class, Views.UserView.Patch.class})
    private String patronymic;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Profile profile = (Profile) o;

        return Objects.equals(id, profile.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}