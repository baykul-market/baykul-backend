package by.baykulbackend.controller.user;

import by.baykulbackend.database.dao.balance.Balance;
import by.baykulbackend.database.dao.user.RefreshToken;
import by.baykulbackend.database.dao.user.User;
import by.baykulbackend.database.dto.security.Views;
import by.baykulbackend.database.repository.balance.IBalanceRepository;
import by.baykulbackend.database.repository.user.IUserRepository;
import by.baykulbackend.exceptions.NotFoundException;
import by.baykulbackend.services.user.AuthService;
import by.baykulbackend.services.user.RefreshTokenService;
import by.baykulbackend.services.user.UserService;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users/profile")
@RequiredArgsConstructor
@Tag(name = "Authenticated User's Profile Management", description = "API for authenticated user's profile operations")
public class ProfileRestController {
    private final IUserRepository iUserRepository;
    private final IBalanceRepository iBalanceRepository;
    private final UserService userService;
    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;

    @Operation(
            summary = "Get user by authentication",
            description = "Retrieves a specific user by authentication principal with their all other information. " +
                    "Requires profile:read permission.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Views.UserFullView.class),
                            examples = @ExampleObject(
                                    name = "Single user response example",
                                    summary = "User details",
                                    value = """
                                            {
                                              "id": "123e4567-e89b-12d3-a456-426614174001",
                                              "createdTs": "2024-01-15T10:30:00",
                                              "updatedTs": "2024-01-20T14:45:30",
                                              "login": "john_doe",
                                              "email": "john.doe@example.com",
                                              "phoneNumber": "+375291234567",
                                              "role": "USER",
                                              "blocked": false,
                                              "refreshTokens": [
                                                {
                                                  "id": "123e4567-e89b-12d3-a456-426614174000",
                                                  "name": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                                  "userAgent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
                                                  "ipAddress": "192.168.1.100"
                                                }
                                              ],
                                              "profile": {
                                                "id": "123e4567-e89b-12d3-a456-426614174010",
                                                "surname": "Doe",
                                                "name": "John",
                                                "patronymic": "Michael"
                                              },
                                              "balance": {
                                                "id": "123e4567-e89b-12d3-a456-426614174010",
                                                "account": 0
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - JWT token missing or invalid",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Unauthorized example",
                                    value = """
                                            {
                                              "error": "Unauthorized",
                                              "message": "Full authentication is required to access this resource"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - insufficient permissions",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Forbidden example",
                                    value = """
                                            {
                                              "error": "Forbidden",
                                              "message": "Access Denied"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Not found example",
                                    value = """
                                            {
                                              "error": "User not found"
                                            }
                                            """
                            )
                    )
            )
    })
    @PreAuthorize("hasAnyAuthority('profile:read')")
    @JsonView(Views.UserFullView.class)
    @GetMapping
    public User getProfile() {
        return iUserRepository.findByLogin(authService.getAuthInfo().getPrincipal().toString())
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    @Operation(
            summary = "Update user by authentication",
            description = "Updates an existing information of user retrieved by authentication principal. " +
                    "Requires profile:write permission.",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "User data to update",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = Views.UserView.Patch.class),
                            examples = @ExampleObject(
                                    name = "Update user request example",
                                    summary = "User update request",
                                    value = """
                                            {
                                              "login": "updated_login",
                                              "email": "updated.email@example.com",
                                              "phoneNumber": "+375293456789",
                                              "password": "newSecurePassword456",
                                              "blocked": false,
                                              "profile": {
                                                "surname": "Doe",
                                                "name": "John",
                                                "patronymic": "Michael"
                                              }
                                            }
                                            """
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User updated successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Update user success example",
                                    summary = "User updated successfully",
                                    value = """
                                            {
                                              "update_user": "true"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - JWT token missing or invalid",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Unauthorized example",
                                    value = """
                                            {
                                              "error": "Unauthorized",
                                              "message": "Full authentication is required to access this resource"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - insufficient permissions",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Forbidden example",
                                    value = """
                                            {
                                              "error": "Forbidden",
                                              "message": "Access Denied"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Not found example",
                                    value = """
                                            {
                                              "error": "User not found",
                                              "message": "User with id 123e4567-e89b-12d3-a456-426614174001 not found"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflict - user with same login/email/phone already exists",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Conflict example",
                                    summary = "Duplicate user data",
                                    value = """
                                            {
                                              "error_login": "User with that login already exists",
                                              "error_email": "User with that email already exists"
                                            }
                                            """
                            )
                    )
            )
    })
    @Transactional
    @PreAuthorize("hasAnyAuthority('profile:write')")
    @PatchMapping
    public ResponseEntity<?> updateProfile(@RequestBody @JsonView(Views.UserView.Patch.class) User user) {
        return userService.updateProfile(user);
    }

    @Operation(
            summary = "Get current user's refresh tokens",
            description = "Retrieves all refresh tokens for the currently authenticated user. " +
                    "Requires profile:read permission.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Current user's refresh tokens retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = Views.RefreshTokenView.Get.class)),
                            examples = @ExampleObject(
                                    name = "Current user tokens response example",
                                    summary = "Current user's refresh tokens",
                                    value = """
                                            [
                                              {
                                                "id": "123e4567-e89b-12d3-a456-426614174000",
                                                "name": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                                "userAgent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
                                                "ipAddress": "192.168.1.100",
                                                "user": {
                                                  "id": "123e4567-e89b-12d3-a456-426614174001",
                                                  "login": "john_doe"
                                                }
                                              },
                                              {
                                                "id": "123e4567-e89b-12d3-a456-426614174002",
                                                "name": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                                "userAgent": "PostmanRuntime/7.29.0",
                                                "ipAddress": "192.168.1.101",
                                                "user": {
                                                  "id": "123e4567-e89b-12d3-a456-426614174001",
                                                  "login": "john_doe"
                                                }
                                              }
                                            ]
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - JWT token missing or invalid",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Unauthorized example",
                                    value = """
                                            {
                                              "error": "Unauthorized",
                                              "message": "Full authentication is required to access this resource"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - insufficient permissions",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Forbidden example",
                                    value = """
                                            {
                                              "error": "Forbidden",
                                              "message": "Access Denied"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Not found example",
                                    value = """
                                            {
                                              "error": "User not found"
                                            }
                                            """
                            )
                    )
            )
    })
    @GetMapping("/refresh-token")
    @PreAuthorize("hasAnyAuthority('profile:read')")
    @JsonView(Views.RefreshTokenView.Get.class)
    public List<RefreshToken> getUserRefreshTokens() {
        return refreshTokenService.findUserRefreshTokens();
    }

    @Operation(
            summary = "Get balance by authentication",
            description = "Retrieves a specific balance by authentication principal with their user and balance history. " +
                    "Requires my-balance:read permission.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Balance retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Views.BalanceFullView.class),
                            examples = @ExampleObject(
                                    name = "Single balance response example",
                                    summary = "Balance details",
                                    value = """
                                            {
                                              "id": "123e4567-e89b-12d3-a456-426614174001",
                                              "createdTs": "2024-01-15T10:30:00",
                                              "updatedTs": "2024-01-20T14:45:30",
                                              "account": 2024.30,
                                              "user": {
                                                "id": "123e4567-e89b-12d3-a456-426614174000",
                                                "login": "john_doe",
                                                "email": "john.doe@example.com",
                                                "profile": {
                                                  "id": "123e4567-e89b-12d3-a456-426614174010",
                                                  "surname": "Doe",
                                                  "name": "John",
                                                  "patronymic": "Michael"
                                                }
                                              },
                                              "balanceHistoryList": [
                                                {
                                                  "id": "30e9276f-ccce-45a7-9c28-e1ce22254eea",
                                                  "amount": 10.00,
                                                  "operationType": "REPLENISHMENT",
                                                  "resultAccount": 20.00
                                                }
                                              ]
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - JWT token missing or invalid",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Unauthorized example",
                                    value = """
                                            {
                                              "error": "Unauthorized",
                                              "message": "Full authentication is required to access this resource"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - insufficient permissions",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Forbidden example",
                                    value = """
                                            {
                                              "error": "Forbidden",
                                              "message": "Access Denied"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Balance not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Not found example",
                                    value = """
                                            {
                                              "error": "User balance not found"
                                            }
                                            """
                            )
                    )
            )
    })
    @GetMapping("/balance")
    @PreAuthorize("hasAnyAuthority('my-balance:read')")
    @JsonView(Views.BalanceFullView.class)
    public Balance getProfileBalance() {
        return iBalanceRepository.findByUserLogin(authService.getAuthInfo().getPrincipal().toString())
                .orElseThrow(() -> new NotFoundException("User balance not found"));
    }
}