package by.baykulbackend.services.user;

import by.baykulbackend.database.dao.user.User;
import by.baykulbackend.database.dto.security.ForgotPasswordRequest;
import by.baykulbackend.database.dao.user.Localization;
import by.baykulbackend.database.repository.user.IRefreshTokenRepository;
import by.baykulbackend.database.repository.user.IUserRepository;
import by.baykulbackend.exceptions.JwtAuthenticationException;
import by.baykulbackend.exceptions.NotFoundException;
import by.baykulbackend.services.email.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.thymeleaf.context.Context;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private IUserRepository userRepository;
    @Mock
    private IRefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setLogin("testuser");
        testUser.setEmail("test@example.com");
        testUser.setLocalization(Localization.ENG);
    }

    @Test
    void forgotPassword_Success_WithLogin() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("testuser");
        when(userRepository.findByLogin("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");

        authService.forgotPassword(request);

        verify(userRepository).save(testUser);
        verify(refreshTokenRepository).deleteByUser(testUser);
        verify(emailService).sendEmail(eq("test@example.com"), anyString(), eq("password-reset"), eq(Localization.ENG), any(Context.class));
    }

    @Test
    void forgotPassword_Success_WithEmail() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("test@example.com");
        when(userRepository.findByLogin("test@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");

        authService.forgotPassword(request);

        verify(userRepository).save(testUser);
        verify(refreshTokenRepository).deleteByUser(testUser);
        verify(emailService).sendEmail(eq("test@example.com"), anyString(), eq("password-reset"), eq(Localization.ENG), any(Context.class));
    }

    @Test
    void forgotPassword_UserNotFound() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("nonexistent");
        when(userRepository.findByLogin("nonexistent")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("nonexistent")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> authService.forgotPassword(request));
        
        verify(userRepository, never()).save(any());
        verify(emailService, never()).sendEmail(any(), any(), any(), any(), any());
    }

    @Test
    void forgotPassword_NoEmailRegistered() {
        testUser.setEmail(null);
        ForgotPasswordRequest request = new ForgotPasswordRequest("testuser");
        when(userRepository.findByLogin("testuser")).thenReturn(Optional.of(testUser));

        assertThrows(JwtAuthenticationException.class, () -> authService.forgotPassword(request));

        verify(userRepository, never()).save(any());
        verify(emailService, never()).sendEmail(any(), any(), any(), any(), any());
    }
}
