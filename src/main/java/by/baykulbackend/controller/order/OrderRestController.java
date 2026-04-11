package by.baykulbackend.controller.order;

import by.baykulbackend.database.dao.order.Order;
import by.baykulbackend.database.dto.security.Views;
import by.baykulbackend.database.repository.order.IOrderRepository;
import by.baykulbackend.exceptions.NotFoundException;
import by.baykulbackend.services.order.OrderService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/order")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Orders management")
public class OrderRestController {
    private final IOrderRepository iOrderRepository;
    private final OrderService orderService;

    @Operation(
            summary = "Get all orders",
            description = "Retrieves all orders from the system with pagination. Requires all-orders:read permission.",
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
                    description = "List of orders retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = Views.OrderView.Get.class)),
                            examples = @ExampleObject(
                                    name = "All orders response example",
                                    summary = "List of all orders",
                                    value = """
                                            [
                                              {
                                                "id": "123e4567-e89b-12d3-a456-426614174001",
                                                "createdTs": "2024-01-15T10:30:00",
                                                "updatedTs": "2024-01-20T14:45:30",
                                                "number": 100001,
                                                "status": "ORDERED"
                                              },
                                              {
                                                "id": "522t4767-e89b-12d3-a456-426614174563",
                                                "createdTs": "2024-01-16T11:30:00",
                                                "updatedTs": "2024-01-21T15:45:30",
                                                "number": 100002,
                                                "status": "ON_WAY"
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
    @PreAuthorize("hasAnyAuthority('all-orders:read')")
    @JsonView(Views.OrderView.Get.class)
    public List<Order> getAll(
            @PageableDefault(size = 50, sort = "createdTs", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return iOrderRepository.findAll(pageable).stream().toList();
    }

    @Operation(
            summary = "Get order by ID",
            description = "Retrieves a specific order by UUID with all details. Requires all-orders:read permission.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Order retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Views.OrderFullView.class),
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
                                                "login": "john_doe",
                                                "email": "john.doe@example.com",
                                                "profile": {
                                                  "id": "123e4567-e89b-12d3-a456-426614174010",
                                                  "surname": "Doe",
                                                  "name": "John",
                                                  "patronymic": "Michael"
                                                }
                                              },
                                              "orderProducts": [
                                                {
                                                  "id": "30e9276f-ccce-45a7-9c28-e1ce22254eea",
                                                  "number": null,
                                                  "status": "TO_ORDER",
                                                  "price": 7862.43,
                                                  "currency": "EUR",
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
    @GetMapping("/id")
    @PreAuthorize("hasAnyAuthority('all-orders:read')")
    @JsonView(Views.OrderFullView.class)
    public Order getOne(
            @Parameter(
                    description = "UUID of the order to retrieve",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000"
            )
            @RequestParam UUID id) {
        return iOrderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order not found"));
    }

    @Operation(
            summary = "Confirm an order",
            description = """
                        Confirms an order that is waiting for confirmation.
                        
                        This operation is used for orders created by users with the 'pay-later' option.
                        When confirmed, the order status changes from CONFIRMATION_WAITING to ORDERED,
                        and product availability is checked:
                        - Products with sufficient stock → IN_WAREHOUSE
                        - Products with insufficient stock → TO_ORDER
                        
                        Requires all-orders:write permission.
                        """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Order confirmed successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Confirm order success example",
                                    value = """
                                            {
                                              "confirmation": "true"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request - order is not in CONFIRMATION_WAITING status",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Bad request example",
                                    value = """
                                            {
                                              "error": "No confirmation needed"
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
    @PostMapping("/confirm")
    @PreAuthorize("hasAnyAuthority('all-orders:write')")
    public ResponseEntity<?> confirm(
            @Parameter(
                    description = "UUID of the order to confirm",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000"
            )
            @RequestParam UUID id
    ) {
        return orderService.confirmOrder(id);
    }

    @Operation(
            summary = "Pay for an order",
            description = """
                        Processes payment for an order.
                        
                        After successful payment if order was not confirmed:
                        - Order status changes to ORDERED
                        - Order is marked as paid
                        - Product availability is checked and statuses are updated
                        
                        Requires all-orders:write permission.
                        """,
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
                    description = "Bad request - order already paid or in invalid status",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Bad request example",
                                    value = """
                                            {
                                              "error": "Order is already paid"
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
    @PostMapping("/pay")
    @PreAuthorize("hasAnyAuthority('all-orders:write')")
    public ResponseEntity<?> pay(
            @Parameter(
                    description = "UUID of the order to pay for",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174001"
            )
            @RequestParam UUID id
    ) {
        return orderService.payForOrder(id);
    }

    @Operation(
            summary = "Complete an order",
            description = """
                        Marks an order as COMPLETED when all products are ready for pickup.
                        
                        **Requirements:**
                        - Order must be in READY_FOR_PICKUP status
                        - Order must be paid
                        
                        When completed:
                        - All order products are marked as DELIVERED
                        - Order status becomes COMPLETED
                        
                        Requires all-orders:write permission.
                        """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Order completed successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Complete order success example",
                                    value = """
                                            {
                                              "complete_order": "true"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request - order not ready for pickup or not paid",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "Not ready example",
                                            value = """
                                                    {
                                                      "error": "Completing order is not allowed: it is not ready"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Not paid example",
                                            value = """
                                                    {
                                                      "error": "Completing order is not allowed: it is not paid"
                                                    }
                                                    """
                                    )
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
    @PostMapping("/complete")
    @PreAuthorize("hasAnyAuthority('all-orders:write')")
    public ResponseEntity<?> complete(
            @Parameter(
                    description = "UUID of the order to complete",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174001"
            )
            @RequestParam UUID id
    ) {
        return orderService.completeOrder(id);
    }

    @Operation(
            summary = "Cancel an order",
            description = """
                        Cancels an order.
                        
                        **Cancellation rules:**
                        - Order can be cancelled at any time by administrator
                        - When cancelled, all order products are marked as CANCELLED
                        - Order status becomes CANCELLED
                        
                        Requires all-orders:write permission.
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
    @PreAuthorize("hasAnyAuthority('all-orders:write')")
    public ResponseEntity<?> cancel(
            @Parameter(
                    description = "UUID of the order to cancel",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174001"
            )
            @RequestParam UUID id
    ) {
        return orderService.cancelOrder(id);
    }
}