package by.baykulbackend.services.user;

import by.baykulbackend.config.PasswordEncoderConfig;
import by.baykulbackend.database.dao.user.Profile;
import by.baykulbackend.database.dao.user.User;
import by.baykulbackend.database.dto.user.UserPatchRequest;
import by.baykulbackend.database.model.Role;
import by.baykulbackend.database.repository.order.IOrderRepository;
import by.baykulbackend.database.repository.user.IRefreshTokenRepository;
import by.baykulbackend.database.repository.user.IUserRepository;
import by.baykulbackend.exceptions.NotFoundException;
import by.baykulbackend.security.JwtAuthentication;
import by.baykulbackend.services.email.EmailService;
import by.baykulbackend.services.finance.PriceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private IUserRepository iUserRepository;
    @Mock
    private IRefreshTokenRepository iRefreshTokenRepository;
    @Mock
    private IOrderRepository iOrderRepository;
    @Mock
    private AuthService authService;
    @Mock
    private PasswordEncoderConfig passwordEncoderConfig;
    @Mock
    private PriceService priceService;
    @Mock
    private EmailService emailService;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User existingUser;
    private UUID userId;
    private JwtAuthentication authInfo;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        existingUser = new User();
        existingUser.setId(userId);
        existingUser.setLogin("oldLogin");
        existingUser.setEmail("old@test.com");
        existingUser.setPhoneNumber("+1234567890");

        authInfo = new JwtAuthentication();
        authInfo.setLogin("adminUser");
    }

    @Test
    void updateUserById_ShouldUpdateFieldsAndSave() {
        when(authService.getAuthInfo()).thenReturn(authInfo);

        UserPatchRequest patch = new UserPatchRequest();
        patch.setLogin("newLogin");
        patch.setRole(Role.ADMIN);
        patch.setCanPayLater(true);
        patch.setMarkupPercentage(Optional.of(new BigDecimal("15.5")));
        
        Profile profile = new Profile();
        profile.setName("John");
        patch.setProfile(profile);

        when(iUserRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(iUserRepository.findByLogin("newLogin")).thenReturn(Optional.empty());

        ResponseEntity<?> response = userService.updateUserById(userId, patch);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("newLogin", existingUser.getLogin());
        assertEquals(Role.ADMIN, existingUser.getRole());
        assertTrue(existingUser.getCanPayLater());
        assertEquals(new BigDecimal("15.5"), existingUser.getMarkupPercentage());
        assertEquals("John", existingUser.getProfile().getName());
        verify(iUserRepository).save(existingUser);
    }

    @Test
    void updateUserById_ShouldClearMarkupPercentage_WhenExplicitlyNull() {
        when(authService.getAuthInfo()).thenReturn(authInfo);

        existingUser.setMarkupPercentage(new BigDecimal("10.0"));

        UserPatchRequest patch = new UserPatchRequest();
        patch.setMarkupPercentage(Optional.empty());

        when(iUserRepository.findById(userId)).thenReturn(Optional.of(existingUser));

        ResponseEntity<?> response = userService.updateUserById(userId, patch);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNull(existingUser.getMarkupPercentage());
        verify(iUserRepository).save(existingUser);
    }

    @Test
    void updateUserById_ShouldNotUpdateMarkup_WhenOmitted() {
        existingUser.setMarkupPercentage(new BigDecimal("10.0"));

        UserPatchRequest patch = new UserPatchRequest();
        patch.setMarkupPercentage(null); // Explicitly omitting

        when(iUserRepository.findById(userId)).thenReturn(Optional.of(existingUser));

        ResponseEntity<?> response = userService.updateUserById(userId, patch);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(new BigDecimal("10.0"), existingUser.getMarkupPercentage());
        // Since no fields changed, save should not be called
        verify(iUserRepository, never()).save(any());
    }

    @Test
    void updateUserById_ShouldReturnConflict_WhenLoginExists() {
        UserPatchRequest patch = new UserPatchRequest();
        patch.setLogin("existingLogin");

        User otherUser = new User();
        otherUser.setId(UUID.randomUUID());

        when(iUserRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(iUserRepository.findByLogin("existingLogin")).thenReturn(Optional.of(otherUser));

        ResponseEntity<?> response = userService.updateUserById(userId, patch);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertTrue(((Map<?, ?>) response.getBody()).containsKey("error_login"));
        verify(iUserRepository, never()).save(any());
    }

    @Test
    void updateUserById_ShouldThrowNotFound_WhenUserDoesNotExist() {
        when(iUserRepository.findById(userId)).thenReturn(Optional.empty());

        UserPatchRequest patch = new UserPatchRequest();

        assertThrows(NotFoundException.class, () -> userService.updateUserById(userId, patch));
    }

    @Test
    void updateProfile_ShouldClearSensitiveFieldsAndDelegate() {
        authInfo.setLogin("userLogin");
        when(authService.getAuthInfo()).thenReturn(authInfo);

        when(iUserRepository.findByLogin("userLogin")).thenReturn(Optional.of(existingUser));
        when(iUserRepository.findById(userId)).thenReturn(Optional.of(existingUser));

        UserPatchRequest patch = new UserPatchRequest();
        patch.setLogin("newLogin");
        patch.setRole(Role.ADMIN); // Should be ignored/cleared
        patch.setBlocked(true); // Should be ignored/cleared
        patch.setMarkupPercentage(Optional.of(new BigDecimal("99.9"))); // Should be ignored/cleared

        ResponseEntity<?> response = userService.updateProfile(patch);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("newLogin", existingUser.getLogin());
        assertNull(existingUser.getRole()); // Because patch.setRole(null) was applied
        assertNull(existingUser.getBlocked());
        assertNull(existingUser.getMarkupPercentage());
        verify(iUserRepository).save(existingUser);
    }
}
