package by.baykulbackend.database.dao.user;

import by.baykulbackend.database.dao.balance.Balance;
import by.baykulbackend.database.dao.cart.Cart;
import by.baykulbackend.database.model.Role;
import by.baykulbackend.database.dto.security.Views;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.CascadeType;
import jakarta.validation.constraints.Email;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Data
@Entity
@JsonIdentityInfo(
        generator = ObjectIdGenerators.PropertyGenerator.class,
        property = "id")
@Table(name = "users")
@Schema(description = "User entity representing system users with authentication and authorization data")
public class User {
    @Schema(
            description = "Unique identifier",
            accessMode = Schema.AccessMode.READ_ONLY,
            example = "123e4567-e89b-12d3-a456-426614174000"
    )
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @JsonView({Views.UserView.Get.class, Views.UserView.Post.class, Views.UserView.Patch.class})
    private UUID id;

    @Schema(
            description = "Timestamp when the user was created",
            accessMode = Schema.AccessMode.READ_ONLY,
            example = "2024-01-15T10:30:00"
    )
    @Column(name = "created_ts")
    @JsonView(Views.UserView.Get.class)
    @CreationTimestamp
    private LocalDateTime createdTs;

    @Schema(
            description = "Timestamp when the user was last updated",
            accessMode = Schema.AccessMode.READ_ONLY,
            example = "2024-01-15T10:30:00"
    )
    @Column(name = "updated_ts")
    @JsonView(Views.UserView.Get.class)
    @UpdateTimestamp
    private LocalDateTime updatedTs;

    @Schema(
            description = "Unique username for authentication",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minLength = 3,
            maxLength = 50,
            example = "john"
    )
    @Column(name = "login", nullable = false, unique = true, length = 50)
    @JsonView({Views.UserView.Get.class, Views.UserView.Post.class, Views.UserView.Patch.class})
    private String login;

    @Schema(
            description = "Hashed password (BCrypt)",
            requiredMode = Schema.RequiredMode.REQUIRED,
            accessMode = Schema.AccessMode.WRITE_ONLY,
            maxLength = 100,
            minLength = 8
    )
    @Column(name = "password", nullable = false, length = 1000)
    @JsonView({Views.UserView.Post.class, Views.UserView.Patch.class})
    private String password;

    @Schema(
            description = "User's email address",
            maxLength = 255,
            format = "email",
            nullable = true,
            example = "example@email.com"
    )
    @Email
    @Column(name = "email", unique = true)
    @JsonView({Views.UserView.Get.class, Views.UserView.Post.class, Views.UserView.Patch.class})
    private String email;

    @Schema(
            description = "User's phone number",
            maxLength = 15,
            minLength = 7,
            format = "phone number",
            nullable = true,
            example = "+375296435434"
    )
    @Column(name = "phone_number", length = 15)
    @JsonView({Views.UserView.Get.class, Views.UserView.Post.class, Views.UserView.Patch.class})
    private String phoneNumber;

    @Schema(
            defaultValue = "USER",
            example = "USER"
    )
    @Column(name = "role", nullable = false)
    @Enumerated(EnumType.STRING)
    @JsonView({Views.UserView.Get.class, Views.UserView.Post.class, Views.UserView.Patch.class})
    private Role role;

    @Schema(
            description = "Indicates if the user account is blocked",
            requiredMode = Schema.RequiredMode.REQUIRED,
            defaultValue = "false",
            example = "false"
    )
    @Column(name = "blocked", nullable = false)
    @JsonView({Views.UserView.Get.class, Views.UserView.Post.class, Views.UserView.Patch.class})
    private Boolean blocked;

    @Schema(
            description = "Indicates user's payment permission",
            defaultValue = "false",
            example = "true"
    )
    @Column(name = "can_pay_later", nullable = false)
    @JsonView({Views.UserAdminView.class, Views.UserView.Post.class, Views.UserView.Patch.class})
    private Boolean canPayLater;

    @Schema(
            description = "Individual markup percentage for user purchases",
            example = "0.10",
            defaultValue = "0.10",
            minimum = "0"
    )
    @Column(name = "markup_percentage", nullable = true)
    @JsonView({Views.UserAdminView.class, Views.UserView.Post.class, Views.UserView.Patch.class})
    private BigDecimal markupPercentage;

    @Schema(
            description = "User's localization preference",
            example = "RUS",
            defaultValue = "RUS",
            allowableValues = {"RUS", "ENG"}
    )
    @Column(name = "localization", nullable = false)
    @Enumerated(EnumType.STRING)
    @JsonView({Views.UserView.Get.class, Views.UserView.Post.class, Views.UserView.Patch.class})
    private Localization localization;

    @Schema(
            description = "List of refresh tokens associated with the user",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonView(Views.UserFullView.class)
    private List<RefreshToken> refreshTokens;

    @Schema(
            description = "User's profile containing personal information",
            example = "{\"id\": \"123e4567-e89b-12d3-a456-426614174000\", \"surname\": \"Surname\"}"
    )
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    @JsonView({Views.UserView.Get.class, Views.UserView.Post.class, Views.UserView.Patch.class})
    private Profile profile;

    @Schema(
            description = "User's balance",
            accessMode = Schema.AccessMode.READ_ONLY,
            example = "{\"id\": \"123e4567-e89b-12d3-a456-426614174000\", \"value\": \"120.00\"}"
    )
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    @JsonView(Views.UserFullView.class)
    private Balance balance;

    @Schema(
            description = "User's cart",
            accessMode = Schema.AccessMode.READ_ONLY,
            example = "{\"id\": \"123e4567-e89b-12d3-a456-426614174000\"}"
    )
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    @JsonView(Views.UserFullView.class)
    private Cart cart;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        User user = (User) o;

        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}