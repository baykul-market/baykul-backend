package by.baykulbackend.controller.user;

import by.baykulbackend.database.dao.user.RefreshToken;
import by.baykulbackend.database.dto.security.Views;
import by.baykulbackend.database.repository.user.IRefreshTokenRepository;
import by.baykulbackend.exceptions.NotFoundException;
import by.baykulbackend.services.user.RefreshTokenService;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/refresh-token")
@RequiredArgsConstructor
@Tag(name = "Refresh Token Management", description = "API for managing user refresh tokens and sessions")
@SecurityRequirement(name = "bearerAuth")
public class RefreshTokenRestController {
    private final IRefreshTokenRepository iRefreshTokenRepository;
    private final RefreshTokenService refreshTokenService;

    @Operation(
            summary = "Get all refresh tokens",
            description = "Retrieves all refresh tokens from the system. Requires users:read permission.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Parameters({
            @Parameter(name = "page", description = "Page number (0-based, default: 0)", example = "0"),
            @Parameter(name = "size", description = "Page size (default: 50)", example = "50"),
            @Parameter(name = "sort", description = "Sort property and direction (e.g., createdTs,desc)", example = "createdTs,desc")
    })
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "List of refresh tokens retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = Views.RefreshTokenView.Get.class)),
                            examples = @ExampleObject(
                                    name = "All tokens response example",
                                    summary = "List of refresh tokens",
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
            )
    })
    @GetMapping
    @PreAuthorize("hasAnyAuthority('users:read')")
    @JsonView(Views.RefreshTokenFullView.class)
    public List<RefreshToken> getAll(
            @PageableDefault(size = 50, sort = "createdTs", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return iRefreshTokenRepository.findAll(pageable).stream().toList();
    }

    @Operation(
            summary = "Get refresh token by ID",
            description = "Retrieves a specific refresh token by its UUID. Requires users:read permission.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Refresh token retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Views.RefreshTokenView.Get.class),
                            examples = @ExampleObject(
                                    name = "Single token response example",
                                    summary = "Refresh token details",
                                    value = """
                                            {
                                              "id": "123e4567-e89b-12d3-a456-426614174000",
                                              "name": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                              "userAgent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
                                              "ipAddress": "192.168.1.100",
                                              "user": {
                                                "id": "123e4567-e89b-12d3-a456-426614174001",
                                                "login": "john_doe"
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
                    description = "Refresh token not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Not found example",
                                    value = """
                                            {
                                              "error": "Refresh token not found"
                                            }
                                            """
                            )
                    )
            )
    })
    @GetMapping("/id")
    @PreAuthorize("hasAnyAuthority('users:read')")
    @JsonView(Views.RefreshTokenFullView.class)
    public RefreshToken getOne(
            @Parameter(
                    description = "UUID of the refresh token to retrieve",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000"
            )
            @RequestParam UUID id) {
        return iRefreshTokenRepository.findById(id).orElseThrow(() -> new NotFoundException("Refresh token not found"));
    }

    @Operation(
            summary = "Get refresh tokens by user ID",
            description = "Retrieves all refresh tokens for a specific user. Requires users:read permission.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User's refresh tokens retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = Views.RefreshTokenView.Get.class)),
                            examples = @ExampleObject(
                                    name = "User tokens response example",
                                    summary = "User's refresh tokens",
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
                                    name = "User not found example",
                                    value = """
                                            {
                                              "error": "User not found",
                                              "message": "User with id 123e4567-e89b-12d3-a456-426614174001 not found"
                                            }
                                            """
                            )
                    )
            )
    })
    @GetMapping("/user")
    @PreAuthorize("hasAnyAuthority('users:read')")
    @JsonView(Views.RefreshTokenView.Get.class)
    public List<RefreshToken> getUserRefTokensByUserId(
            @Parameter(
                    description = "UUID of the user",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174001"
            )
            @RequestParam UUID userId) {
        return refreshTokenService.findUserRefTokensByUserId(userId);
    }

    @Operation(
            summary = "Delete refresh token",
            description = "Deletes a refresh token by ID. User can only delete their own tokens unless they are ADMIN. " +
                    "Requires profile:write permission.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Refresh token deleted successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Delete success example",
                                    summary = "Successful deletion",
                                    value = """
                                            {
                                              "delete_refresh_token": "true"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request - user doesn't own the token",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Bad request example",
                                    summary = "Token ownership error",
                                    value = """
                                            {
                                              "delete_refresh_token": "false"
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
                    description = "Refresh token not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Not found example",
                                    value = """
                                            {
                                              "error": "Refresh token not found",
                                              "message": "Refresh token with id 123e4567-e89b-12d3-a456-426614174000 not found"
                                            }
                                            """
                            )
                    )
            )
    })
    @DeleteMapping("/id")
    @PreAuthorize("hasAnyAuthority('profile:write')")
    public ResponseEntity<?> delete(
            @Parameter(
                    description = "UUID of the refresh token to delete",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000"
            )
            @RequestParam UUID id) {
        return refreshTokenService.deleteById(id);
    }
}