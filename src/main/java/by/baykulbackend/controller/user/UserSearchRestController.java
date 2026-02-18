package by.baykulbackend.controller.user;

import by.baykulbackend.database.dao.user.User;
import by.baykulbackend.database.dto.security.Views;
import by.baykulbackend.database.repository.user.IUserRepository;
import by.baykulbackend.exceptions.NotFoundException;
import by.baykulbackend.services.user.UserService;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameters;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users/search")
@RequiredArgsConstructor
@Tag(name = "User Search", description = "API for user search operations")
public class UserSearchRestController {
    private final IUserRepository iUserRepository;
    private final UserService userService;

    @Operation(
            summary = "Search users",
            description = "Searches users by login, email, or phone number containing the specified text with pagination. " +
                    "Requires users:write permission.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Parameters({
            @Parameter(name = "text", description = "Text to search for in login, email, or phone number", required = true, example = "john"),
            @Parameter(name = "page", description = "Page number (0-based, default: 0)", example = "0"),
            @Parameter(name = "size", description = "Page size (default: 50)", example = "50"),
            @Parameter(name = "sort", description = "Sort property and direction (e.g., createdTs,desc)", example = "createdTs,desc")
    })
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Search results retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = Views.UserView.Get.class)),
                            examples = @ExampleObject(
                                    name = "Search results example",
                                    summary = "Search results",
                                    value = """
                                        [
                                          {
                                            "id": "123e4567-e89b-12d3-a456-426614174001",
                                            "createdTs": "2024-01-15T10:30:00",
                                            "updatedTs": "2024-01-20T14:45:30",
                                            "login": "john_doe",
                                            "email": "john.doe@example.com",
                                            "phoneNumber": "+375291234567",
                                            "role": "USER",
                                            "blocked": false,
                                            "profile": {
                                              "id": "123e4567-e89b-12d3-a456-426614174010",
                                              "surname": "Doe",
                                              "name": "John",
                                              "patronymic": "Michael"
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
    @PreAuthorize("hasAnyAuthority('users:write')")
    @JsonView(Views.UserView.Get.class)
    @GetMapping
    public List<User> search(
            @RequestParam(required = false, defaultValue = "") String text,
            @PageableDefault(size = 20, sort = "createdTs", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return userService.searchUser(text, pageable).stream().toList();
    }

    @Operation(
            summary = "Get user by login",
            description = "Retrieves a specific user by exact login with their refresh tokens. " +
                    "Requires users:write permission.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Views.UserView.Get.class),
                            examples = @ExampleObject(
                                    name = "Single user response example",
                                    summary = "User details",
                                    value = """
                                            {
                                              "id": "123e4567-e89b-12d3-a456-426614174001",
                                              "createdTs": "2024-01-15T10:30:00",
                                              "updatedTs": "2024-01-20T14:45:30",
                                              "login": "john",
                                              "email": "john.doe@example.com",
                                              "phoneNumber": "+375291234567",
                                              "role": "USER",
                                              "blocked": false,
                                              "profile": {
                                                "id": "123e4567-e89b-12d3-a456-426614174010",
                                                "surname": "Doe",
                                                "name": "John",
                                                "patronymic": "Michael"
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
    @PreAuthorize("hasAnyAuthority('users:write')")
    @JsonView(Views.UserView.Get.class)
    @GetMapping("/exact/login")
    public User getByLogin(
            @Parameter(
                    description = "Login text to search for",
                    required = true,
                    example = "john"
            )
            @RequestParam String login) {
        return iUserRepository.findByLogin(login).orElseThrow(() -> new NotFoundException("User not found"));
    }

    @Operation(
            summary = "Search users by login",
            description = "Searches users by login containing the specified text (case-insensitive) with pagination. " +
                    "Requires users:write permission.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Parameters({
            @Parameter(name = "login", description = "Login text to search for (case-insensitive)", required = true, example = "john"),
            @Parameter(name = "page", description = "Page number (0-based, default: 0)", example = "0"),
            @Parameter(name = "size", description = "Page size (default: 50)", example = "50"),
            @Parameter(name = "sort", description = "Sort property and direction (e.g., createdTs,desc)", example = "createdTs,desc")
    })
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Search results retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = Views.UserView.Get.class)),
                            examples = @ExampleObject(
                                    name = "Search by login results example",
                                    summary = "Search by login results",
                                    value = """
                                            [
                                              {
                                                "id": "123e4567-e89b-12d3-a456-426614174001",
                                                "createdTs": "2024-01-15T10:30:00",
                                                "updatedTs": "2024-01-20T14:45:30",
                                                "login": "john_doe",
                                                "email": "john.doe@example.com",
                                                "phoneNumber": "+375291234567",
                                                "role": "USER",
                                                "blocked": false,
                                                "profile": {
                                                  "id": "123e4567-e89b-12d3-a456-426614174010",
                                                  "surname": "Doe",
                                                  "name": "John",
                                                  "patronymic": "Michael"
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
    @PreAuthorize("hasAnyAuthority('users:write')")
    @JsonView(Views.UserView.Get.class)
    @GetMapping("/login")
    public List<User> searchByLogin(
            @RequestParam String login,
            @PageableDefault(size = 20, sort = "createdTs", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return iUserRepository.findByLoginContainingIgnoreCase(login, pageable).stream().toList();
    }

    @Operation(
            summary = "Get user by email",
            description = "Retrieves a specific user by exact email with their refresh tokens. " +
                    "Requires users:write permission.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Views.UserView.Get.class),
                            examples = @ExampleObject(
                                    name = "Single user response example",
                                    summary = "User details",
                                    value = """
                                            {
                                              "id": "123e4567-e89b-12d3-a456-426614174001",
                                              "createdTs": "2024-01-15T10:30:00",
                                              "updatedTs": "2024-01-20T14:45:30",
                                              "login": "john",
                                              "email": "john.doe@example.com",
                                              "phoneNumber": "+375291234567",
                                              "role": "USER",
                                              "blocked": false,
                                              "profile": {
                                                "id": "123e4567-e89b-12d3-a456-426614174010",
                                                "surname": "Doe",
                                                "name": "John",
                                                "patronymic": "Michael"
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
    @PreAuthorize("hasAnyAuthority('users:write')")
    @JsonView(Views.UserView.Get.class)
    @GetMapping("/exact/email")
    public User getByEmail(
            @Parameter(
                    description = "Email text to search for",
                    required = true,
                    example = "john.doe@example.com"
            )
            @RequestParam String email) {
        return iUserRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("User not found"));
    }

    @Operation(
            summary = "Search users by email",
            description = "Searches users by email containing the specified text (case-insensitive) with pagination. " +
                    "Requires users:write permission.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Parameters({
            @Parameter(name = "email", description = "Email text to search for (case-insensitive)", required = true, example = "example.com"),
            @Parameter(name = "page", description = "Page number (0-based, default: 0)", example = "0"),
            @Parameter(name = "size", description = "Page size (default: 50)", example = "50"),
            @Parameter(name = "sort", description = "Sort property and direction (e.g., createdTs,desc)", example = "createdTs,desc")
    })
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Search results retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = Views.UserView.Get.class)),
                            examples = @ExampleObject(
                                    name = "Search by email results example",
                                    summary = "Search by email results",
                                    value = """
                                            [
                                              {
                                                "id": "123e4567-e89b-12d3-a456-426614174001",
                                                "createdTs": "2024-01-15T10:30:00",
                                                "updatedTs": "2024-01-20T14:45:30",
                                                "login": "john_doe",
                                                "email": "john.doe@example.com",
                                                "phoneNumber": "+375291234567",
                                                "role": "USER",
                                                "blocked": false,
                                                "profile": {
                                                  "id": "123e4567-e89b-12d3-a456-426614174010",
                                                  "surname": "Doe",
                                                  "name": "John",
                                                  "patronymic": "Michael"
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
    @PreAuthorize("hasAnyAuthority('users:write')")
    @JsonView(Views.UserView.Get.class)
    @GetMapping("/email")
    public List<User> searchByEmail(
            @RequestParam String email,
            @PageableDefault(size = 20, sort = "createdTs", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return iUserRepository.findByEmailContainingIgnoreCase(email, pageable).stream().toList();
    }

    @Operation(
            summary = "Get user by phone number",
            description = "Retrieves a specific user by exact phone number with their refresh tokens. " +
                    "Requires users:write permission.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Views.UserView.Get.class),
                            examples = @ExampleObject(
                                    name = "Single user response example",
                                    summary = "User details",
                                    value = """
                                            {
                                              "id": "123e4567-e89b-12d3-a456-426614174001",
                                              "createdTs": "2024-01-15T10:30:00",
                                              "updatedTs": "2024-01-20T14:45:30",
                                              "login": "john",
                                              "email": "john.doe@example.com",
                                              "phoneNumber": "+375291234567",
                                              "role": "USER",
                                              "blocked": false,
                                              "profile": {
                                                "id": "123e4567-e89b-12d3-a456-426614174010",
                                                "surname": "Doe",
                                                "name": "John",
                                                "patronymic": "Michael"
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
    @PreAuthorize("hasAnyAuthority('users:write')")
    @JsonView(Views.UserView.Get.class)
    @GetMapping("/exact/phoneNumber")
    public User getByPhoneNumber(
            @Parameter(
                    description = "Phone number text to search for",
                    required = true,
                    example = "+375291234567"
            )
            @RequestParam String phoneNumber) {
        return iUserRepository.findByPhoneNumber(phoneNumber).orElseThrow(() -> new NotFoundException("User not found"));
    }

    @Operation(
            summary = "Search users by phone number",
            description = "Searches users by phone number containing the specified text with pagination. " +
                    "Requires users:write permission.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Parameters({
            @Parameter(name = "phoneNumber", description = "Phone number text to search for", required = true, example = "291234567"),
            @Parameter(name = "page", description = "Page number (0-based, default: 0)", example = "0"),
            @Parameter(name = "size", description = "Page size (default: 50)", example = "50"),
            @Parameter(name = "sort", description = "Sort property and direction (e.g., createdTs,desc)", example = "createdTs,desc")
    })
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Search results retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = Views.UserView.Get.class)),
                            examples = @ExampleObject(
                                    name = "Search by phone results example",
                                    summary = "Search by phone results",
                                    value = """
                                            [
                                              {
                                                "id": "123e4567-e89b-12d3-a456-426614174001",
                                                "createdTs": "2024-01-15T10:30:00",
                                                "updatedTs": "2024-01-20T14:45:30",
                                                "login": "john_doe",
                                                "email": "john.doe@example.com",
                                                "phoneNumber": "+375291234567",
                                                "role": "USER",
                                                "blocked": false,
                                                "profile": {
                                                  "id": "123e4567-e89b-12d3-a456-426614174010",
                                                  "surname": "Doe",
                                                  "name": "John",
                                                  "patronymic": "Michael"
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
    @PreAuthorize("hasAnyAuthority('users:write')")
    @JsonView(Views.UserView.Get.class)
    @GetMapping("/phoneNumber")
    public List<User> searchByPhoneNumber(
            @RequestParam String phoneNumber,
            @PageableDefault(size = 20, sort = "createdTs", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return iUserRepository.findByPhoneNumberContaining(phoneNumber, pageable).stream().toList();
    }
}