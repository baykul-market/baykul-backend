package by.baykulbackend.controller.user;

import by.baykulbackend.database.dao.user.User;
import by.baykulbackend.database.dto.security.Views;
import by.baykulbackend.database.repository.user.IUserRepository;
import by.baykulbackend.exceptions.NotFoundException;
import by.baykulbackend.services.user.UserService;
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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "API for user registration and management")
public class UserRestController {
    private final IUserRepository iUserRepository;
    private final UserService userService;

    @Operation(
            summary = "Get all users",
            description = "Retrieves all users from the system with their refresh tokens. " +
                    "Requires users:read permission.",
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
                    description = "List of users retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = Views.UserView.Get.class)),
                            examples = @ExampleObject(
                                    name = "All users response example",
                                    summary = "List of all users",
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
                                              },
                                              {
                                                "id": "123e4567-e89b-12d3-a456-426614174002",
                                                "createdTs": "2024-01-16T09:15:00",
                                                "updatedTs": "2024-01-19T11:20:00",
                                                "login": "jane_smith",
                                                "email": "jane.smith@example.com",
                                                "phoneNumber": "+375292345678",
                                                "role": "ADMIN",
                                                "blocked": false,
                                                "profile": {
                                                  "id": "123e4567-e89b-12d3-a456-426614174011",
                                                  "surname": "Smith",
                                                  "name": "Jane",
                                                  "patronymic": "Ann"
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
    @PreAuthorize("hasAnyAuthority('users:read')")
    @JsonView(Views.UserAdminView.class)
    @GetMapping
    public List<User> getAll(
            @PageableDefault(size = 50, sort = "createdTs", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return iUserRepository.findAll(pageable).stream().toList();
    }

    @Operation(
            summary = "Get user by ID",
            description = "Retrieves a specific user by UUID with their refresh tokens. " +
                    "Requires users:read permission.",
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
    @PreAuthorize("hasAnyAuthority('users:read')")
    @JsonView(Views.UserAdminView.class)
    @GetMapping("/id")
    public User getOne(
            @Parameter(
                    description = "UUID of the user to retrieve",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174001"
            )
            @RequestParam UUID id) {
        return iUserRepository.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
    }

    @Operation(
            summary = "Create new user",
            description = "Creates a new user in the system. Requires users:write permission.",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "User data to create",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = Views.UserView.Post.class),
                            examples = @ExampleObject(
                                    name = "Create user request example",
                                    summary = "User creation request",
                                    value = """
                                            {
                                              "login": "new_user",
                                              "password": "securePassword123",
                                              "email": "new.user@example.com",
                                              "phoneNumber": "+375292345678",
                                              "role": "USER",
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
                    description = "User created successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Create user success example",
                                    summary = "User created successfully",
                                    value = """
                                            {
                                              "create_user": "true",
                                              "id": "123e4567-e89b-12d3-a456-426614174003"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request - invalid user data",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Validation error example",
                                    summary = "Validation errors",
                                    value = """
                                            {
                                              "error_login": "The login must not be empty",
                                              "error_password": "The password must not be empty"
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
    @PreAuthorize("hasAnyAuthority('users:write')")
    @PostMapping
    public ResponseEntity<?> create(@RequestBody @JsonView(Views.UserView.Post.class) User user) {
        return userService.createUser(user);
    }

    @Operation(
            summary = "Register new user",
            description = "Registers a new user in the system. No authentication required. Returns validation errors if registration fails.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "User registration data",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = Views.UserView.Post.class),
                            examples = @ExampleObject(
                                    name = "Registration request example",
                                    summary = "User registration request",
                                    value = """
                                            {
                                              "login": "new_user",
                                              "password": "securePassword123",
                                              "email": "new.user@example.com",
                                              "phoneNumber": "+375292345678",
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
                    description = "User registered successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Registration success example",
                                    summary = "User registered successfully",
                                    value = """
                                            {
                                              "registration_user": "true",
                                              "id": "123e4567-e89b-12d3-a456-426614174003"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request - validation errors",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Registration validation error example",
                                    summary = "Registration validation errors",
                                    value = """
                                            {
                                              "error_login": "The login must not be empty",
                                              "error_data": "One of the following must be filled in: email, phone number"
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
                                    name = "Registration conflict example",
                                    summary = "Duplicate registration data",
                                    value = """
                                            {
                                              "error_login": "User with that login already exists",
                                              "error_phone_number": "User with that phone number already exists"
                                            }
                                            """
                            )
                    )
            )
    })
    @PostMapping("/registration")
    public ResponseEntity<?> registration(@RequestBody @JsonView(Views.UserView.Post.class) User user) {
        return userService.registerUser(user);
    }

    @Operation(
            summary = "Update user",
            description = "Updates an existing user's information. Requires users:write permission.",
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
    @PreAuthorize("hasAnyAuthority('users:write')")
    @PatchMapping("/id")
    public ResponseEntity<?> update(
            @Parameter(
                    description = "UUID of the user to update",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174001"
            )
            @RequestParam UUID id,
            @RequestBody @JsonView(Views.UserView.Patch.class) User user) {
        return userService.updateUserById(id, user);
    }

    @Operation(
            summary = "Delete user",
            description = "Deletes a user by ID. Requires users:write permission.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User deleted successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Delete user success example",
                                    summary = "User deleted successfully",
                                    value = """
                                            {
                                              "delete_user": "true"
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
            )
    })
    @PreAuthorize("hasAnyAuthority('users:write')")
    @DeleteMapping("/id")
    public ResponseEntity<?> delete(
            @Parameter(
                    description = "UUID of the user to delete",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174001"
            )
            @RequestParam UUID id) {
        return userService.deleteUserById(id);
    }
}