package by.baykulbackend.controller.user;

import by.baykulbackend.services.user.AuthService;
import by.baykulbackend.database.dto.security.JwtRequest;
import by.baykulbackend.database.dto.security.JwtResponse;
import by.baykulbackend.database.dto.security.RefreshJwtRequest;
import by.baykulbackend.services.user.RefreshTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User authentication and token management")
public class AuthenticationRestController {
    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;

    @Operation(
            summary = "User login",
            description = "Authenticate user with login and password. Returns JWT access and refresh tokens.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "User credentials",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = JwtRequest.class),
                            examples = @ExampleObject(
                                    name = "Login example",
                                    summary = "User login example",
                                    value = """
                                            {
                                              "login": "john_doe",
                                              "password": "securePassword123"
                                            }
                                            """
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Authentication successful",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = JwtResponse.class),
                            examples = @ExampleObject(
                                    name = "Login success response",
                                    summary = "Successful login response",
                                    value = """
                                            {
                                              "type": "Bearer",
                                              "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                              "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request - invalid input or missing required fields",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Bad request example",
                                    summary = "Validation error",
                                    value = """
                                            {
                                              "error": "Login and password are required"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - invalid credentials or user blocked",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Forbidden example",
                                    summary = "Authentication failed",
                                    value = """
                                            {
                                              "error": "Invalid credentials"
                                            }
                                            """
                            )
                    )
            )
    })
    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(
            @Schema(
                    description = "Client/browser identifier",
                    example = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
            )
            @RequestHeader(value = "User-Agent")
            String userAgent,
            HttpServletRequest request,
            @Valid @RequestBody JwtRequest authRequest) {
        final JwtResponse token = authService.login(userAgent, request, authRequest);

        return ResponseEntity.ok(token);
    }

    @Operation(
            summary = "Get new access token",
            description = "Generate a new access token using a valid refresh token. " +
                    "Used when access token expires. Returns only access token.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Refresh Token",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RefreshJwtRequest.class),
                            examples = @ExampleObject(
                                    name = "Refresh token request example",
                                    summary = "Refresh token request",
                                    value = """
                                            {
                                              "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
                                            }
                                            """
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "New access token generated successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = JwtResponse.class),
                            examples = @ExampleObject(
                                    name = "Access token response example",
                                    summary = "Successful token refresh",
                                    value = """
                                            {
                                              "type": "Bearer",
                                              "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                              "refreshToken": null
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request - invalid refresh token format or missing token",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Bad request example",
                                    summary = "Invalid token format",
                                    value = """
                                            {
                                              "error": "Refresh token is required"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - invalid, expired, or non-existent refresh token",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Forbidden example",
                                    summary = "Invalid refresh token",
                                    value = """
                                            {
                                              "error": "Refresh token is invalid or expired"
                                            }
                                            """
                            )
                    )
            )
    })
    @PostMapping("/token")
    public ResponseEntity<JwtResponse> getNewAccessToken(@Valid @RequestBody RefreshJwtRequest request,
                                                         HttpServletRequest httpServletRequest) {
        final JwtResponse token = authService.getAccessToken(request.getRefreshToken(), httpServletRequest);

        return ResponseEntity.ok(token);
    }

    @Operation(
            summary = "Refresh both tokens",
            description = "Generate new pair of access and refresh tokens. Invalidates the old refresh token.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Refresh Token",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RefreshJwtRequest.class),
                            examples = @ExampleObject(
                                    name = "Refresh both tokens request example",
                                    summary = "Refresh tokens request",
                                    value = """
                                            {
                                              "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
                                            }
                                            """
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "New token pair generated successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = JwtResponse.class),
                            examples = @ExampleObject(
                                    name = "Token refresh response example",
                                    summary = "Successful token pair refresh",
                                    value = """
                                            {
                                              "type": "Bearer",
                                              "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                              "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request - invalid refresh token format",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Bad request example",
                                    summary = "Invalid token format",
                                    value = """
                                            {
                                              "error": "Invalid refresh token format"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - invalid, expired, or non-existent refresh token",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Forbidden example",
                                    summary = "Invalid token",
                                    value = """
                                            {
                                              "error": "JWT token is invalid"
                                            }
                                            """
                            )
                    )
            )
    })
    @PostMapping("/refresh")
    public ResponseEntity<JwtResponse> getNewRefreshToken(@Valid @RequestBody RefreshJwtRequest request,
                                                          HttpServletRequest httpServletRequest) {
        final JwtResponse token = authService.refresh(request.getRefreshToken(), httpServletRequest);

        return ResponseEntity.ok(token);
    }

    @Operation(
            summary = "User logout",
            description = "Invalidate refresh token and clear security context. Requires valid refresh token.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Refresh Token",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RefreshJwtRequest.class),
                            examples = @ExampleObject(
                                    name = "Logout request example",
                                    summary = "Logout request",
                                    value = """
                                            {
                                              "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
                                            }
                                            """
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "Logout successful - no content returned"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request - invalid refresh token format",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Bad request example",
                                    summary = "Invalid token format",
                                    value = """
                                            {
                                              "error": "Refresh token is required"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Not found - refresh token doesn't exist in database",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Not found example",
                                    summary = "Token not found",
                                    value = """
                                            {
                                              "error": "Refresh token not found"
                                            }
                                            """
                            )
                    )
            )
    })
    @PostMapping("/logout")
    public void logout(@Valid @RequestBody RefreshJwtRequest refreshToken,
                       HttpServletRequest request, HttpServletResponse response) {
        refreshTokenService.deleteByName(refreshToken.getRefreshToken());
        SecurityContextLogoutHandler securityContextLogoutHandler = new SecurityContextLogoutHandler();
        securityContextLogoutHandler.logout(request, response, null);
    }
}