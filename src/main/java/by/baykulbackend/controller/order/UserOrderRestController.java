package by.baykulbackend.controller.order;

import by.baykulbackend.database.dao.order.Order;
import by.baykulbackend.database.dto.security.Views;
import by.baykulbackend.exceptions.NotFoundException;
import by.baykulbackend.database.repository.order.IOrderRepository;
import by.baykulbackend.services.order.OrderService;
import by.baykulbackend.services.user.AuthService;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/order/user")
@RequiredArgsConstructor
@Tag(name = "User's orders", description = "User's orders management")
public class UserOrderRestController {
    private final OrderService orderService;
    private final IOrderRepository iOrderRepository;
    private final AuthService authService;

    @Operation(
            summary = "Get user's orders",
            description = "Retrieves all orders of the currently authenticated user with pagination. " +
                    "Requires my-orders:read permission.",
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
                    description = "Orders retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Views.OrderFullView.class),
                            examples = @ExampleObject(
                                    name = "User orders response example",
                                    summary = "User's orders list",
                                    value = """
                                            [
                                              {
                                                "id": "123e4567-e89b-12d3-a456-426614174001",
                                                "createdTs": "2024-01-15T10:30:00",
                                                "updatedTs": "2024-01-20T14:45:30",
                                                "number": 100001,
                                                "status": "ORDERED",
                                                "user": {
                                                  "id": "123e4567-e89b-12d3-a456-426614174000",
                                                  "login": "john_doe"
                                                },
                                                "orderProducts": [
                                                  {
                                                    "id": "30e9276f-ccce-45a7-9c28-e1ce22254eea",
                                                    "number": null,
                                                    "status": "TO_ORDER",
                                                    "part": {
                                                      "id": "63e9276f-ccce-45a7-9c28-e1ce24354eea",
                                                      "article": "2405947",
                                                      "name": "Engine Oil LL01 5W30",
                                                      "weight": 150.4,
                                                      "minCount": 3,
                                                      "storageCount": 5,
                                                      "returnPart": 3.01,
                                                      "price": 7862.43,
                                                      "currency": "EUR",
                                                      "brand": "rolls royce"
                                                    },
                                                    "partsCount": 3
                                                  }
                                                ]
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
    @PreAuthorize("hasAnyAuthority('my-orders:read')")
    @JsonView(Views.OrderFullView.class)
    public List<Order> getAll(
            @PageableDefault(size = 50, sort = "createdTs", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return iOrderRepository.findByUserLogin(
                authService.getAuthInfo().getPrincipal().toString(),
                pageable
        ).stream().toList();
    }

    @Operation(
            summary = "Get user's order by ID",
            description = "Retrieves a specific order by ID for the currently authenticated user. " +
                    "Requires my-orders:read permission.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Order retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Order.class),
                            examples = @ExampleObject(
                                    name = "Single order response example",
                                    summary = "Order details",
                                    value = """
                                            {
                                              "id": "123e4567-e89b-12d3-a456-426614174001",
                                              "createdTs": "2024-01-15T10:30:00",
                                              "updatedTs": "2024-01-20T14:45:30",
                                              "number": 100001,
                                              "status": "ORDERED",
                                              "user": {
                                                "id": "123e4567-e89b-12d3-a456-426614174000",
                                                "login": "john_doe"
                                              },
                                              "orderProducts": [
                                                {
                                                  "id": "30e9276f-ccce-45a7-9c28-e1ce22254eea",
                                                  "number": null,
                                                  "status": "TO_ORDER",
                                                  "part": {
                                                    "id": "63e9276f-ccce-45a7-9c28-e1ce24354eea",
                                                    "article": "2405947",
                                                    "name": "Engine Oil LL01 5W30",
                                                    "weight": 150.4,
                                                    "minCount": 3,
                                                    "storageCount": 5,
                                                    "returnPart": 3.01,
                                                    "price": 7862.43,
                                                    "currency": "EUR",
                                                    "brand": "rolls royce"
                                                  },
                                                  "partsCount": 3
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
                    description = "Forbidden - insufficient permissions or order doesn't belong to user",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Forbidden example",
                                    value = """
                                            {
                                              "error": "Access denied",
                                              "message": "This order does not belong to you"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Order not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Not found example",
                                    value = """
                                            {
                                              "error": "Order not found"
                                            }
                                            """
                            )
                    )
            )
    })
    @GetMapping("/id")
    @PreAuthorize("hasAnyAuthority('my-orders:read')")
    @JsonView(Views.OrderFullView.class)
    public Order getOne(
            @Parameter(
                    description = "UUID of the order to retrieve",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000"
            )
            @RequestParam UUID id) {
        return iOrderRepository.findByUserLoginAndId(authService.getAuthInfo().getPrincipal().toString(), id)
                .orElseThrow(() -> new NotFoundException("Order not found"));
    }

    @Operation(
            summary = "Create order from cart",
            description = "Creates a new order from the user's cart with availability check. " +
                    "Requires my-orders:write permission.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Order created successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Create order success example",
                                    summary = "Order created successfully",
                                    value = """
                                            {
                                              "create_order": "true",
                                              "id": "123e4567-e89b-12d3-a456-426614174003"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request - cart is empty",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Bad request example",
                                    value = """
                                            {
                                              "error": "Cart is empty"
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
                    description = "Not found - user or cart not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Not found example",
                                    value = """
                                            {
                                              "error": "Cart not found"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflict - products unavailable",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Conflict example",
                                    summary = "Products unavailable",
                                    value = """
                                            {
                                              "create_order": "false",
                                              "error": "No products available in storage",
                                              "unavailable_products": [
                                                {
                                                  "part_id": "123e4567-e89b-12d3-a456-426614174001"
                                                }
                                              ]
                                            }
                                            """
                            )
                    )
            )
    })
    @PostMapping("/create")
    @PreAuthorize("hasAnyAuthority('my-orders:write')")
    public ResponseEntity<?> create() {
        return orderService.createOrderFromCart();
    }

    @Operation(
            summary = "Pay for an existing order",
            description = "Processes payment for user's order that was created" +
                    "Requires my-orders:write permission.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Order paid successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Pay order success example",
                                    value = """
                                            {
                                              "pay_order": "true"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request - order not found, not owned by user, or already paid",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Bad request example",
                                    value = """
                                            {
                                              "error": "Order is already paid or processed"
                                            }
                                            """
                            )
                    )
            )
    })
    @PostMapping("/pay")
    @PreAuthorize("hasAnyAuthority('my-orders:write')")
    public ResponseEntity<?> pay(
            @Parameter(
                    description = "UUID of the order to pay for",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174001"
            )
            @RequestParam UUID id) {
        return orderService.payForUsersOrder(id);
    }

    @Operation(
            summary = "Cancel user's order",
            description = """
                    Cancels an order belonging to the current user.
                    
                    **Cancellation rules for users:**
                    - Order must belong to the current user
                    - Order can only be cancelled if it's in:
                      - CONFIRMATION_WAITING (waiting for user confirmation)
                      - PAYMENT_WAITING (waiting for payment)
                    
                    After these stages, users cannot cancel orders themselves
                    (requires administrator intervention).
                    
                    When cancelled:
                    - All order products are marked as CANCELLED
                    - Order status becomes CANCELLED
                    
                    Requires my-orders:write permission.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Order cancelled successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Cancel order success example",
                                    value = """
                                            {
                                              "cancel_order": "true"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = """
                            Bad request. Possible reasons:
                            - Order does not belong to current user
                            - Order is not in a cancellable state (only CONFIRMATION_WAITING or PAYMENT_WAITING can be cancelled by user)
                            """,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "Not owner example",
                                            value = """
                                                    {
                                                      "error": "Order does not belong to the current user"
                                                    }
                                                    """),
                                    @ExampleObject(
                                            name = "Cannot cancel example",
                                            value = """
                                                    {
                                                      "error": "Cancelling order is not allowed"
                                                    }
                                                    """)
                            }
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
                    description = "Order not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Not found example",
                                    value = """
                                            {
                                              "error": "Order not found"
                                            }
                                            """
                            )
                    )
            )
    })
    @PostMapping("/cancel")
    @PreAuthorize("hasAnyAuthority('my-orders:write')")
    public ResponseEntity<?> cancel(
            @Parameter(
                    description = "UUID of the order to pay for",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174001"
            )
            @RequestParam UUID id
    ) {
        return orderService.cancelUsersOrder(id);
    }
}