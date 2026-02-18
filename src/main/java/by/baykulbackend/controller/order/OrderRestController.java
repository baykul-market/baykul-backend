package by.baykulbackend.controller.order;

import by.baykulbackend.database.dao.order.Order;
import by.baykulbackend.database.dao.order.OrderProduct;
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
import org.springframework.web.bind.annotation.*;

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
            description = "Retrieves all orders from the system with pagination. Requires orders:write permission.",
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
                                                "status": "CREATED"
                                              },
                                              {
                                                "id": "522t4767-e89b-12d3-a456-426614174563",
                                                "createdTs": "2024-01-16T11:30:00",
                                                "updatedTs": "2024-01-21T15:45:30",
                                                "number": 100002,
                                                "status": "PROCESSING"
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
    @PreAuthorize("hasAnyAuthority('orders:write')")
    @JsonView(Views.OrderView.Get.class)
    public List<Order> getAll(
            @PageableDefault(size = 20, sort = "createdTs", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return iOrderRepository.findAll(pageable).stream().toList();
    }

    @Operation(
            summary = "Get order by ID",
            description = "Retrieves a specific order by UUID with all details. Requires orders:write permission.",
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
                                              "status": "CREATED",
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
                                                  "status": "ORDERED",
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
    @PreAuthorize("hasAnyAuthority('orders:write')")
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
            summary = "Update order",
            description = "Updates an order (status). Requires orders:write permission.",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Order data to update",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = Order.class),
                            examples = @ExampleObject(
                                    name = "Update order request example",
                                    summary = "Update order status",
                                    value = """
                                            {
                                              "status": "PAID"
                                            }
                                            """
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Order updated successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Update order success example",
                                    summary = "Order updated successfully",
                                    value = """
                                            {
                                              "update_order": "true"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request - invalid order data",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Validation error example",
                                    summary = "Validation errors",
                                    value = """
                                            {
                                              "error": "Invalid status value"
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
    @PutMapping
    @PreAuthorize("hasAnyAuthority('orders:write')")
    public ResponseEntity<?> update(
            @Parameter(
                    description = "UUID of the order to update",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000"
            )
            @RequestParam UUID id,
            @RequestBody Order order) {
        return orderService.updateOrder(id, order);
    }

    @Operation(
            summary = "Update order product",
            description = "Updates an order product (number, status, parts count). Requires orders:write permission.",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Order product data to update",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = OrderProduct.class),
                            examples = @ExampleObject(
                                    name = "Update order product request example",
                                    summary = "Update order product details",
                                    value = """
                                            {
                                              "number": 100001,
                                              "status": "IN_WAREHOUSE",
                                              "partsCount": 5
                                            }
                                            """
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Order product updated successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Update order product success example",
                                    summary = "Order product updated successfully",
                                    value = """
                                            {
                                              "update_order_product": "true"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request - invalid order product data",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Validation error example",
                                    summary = "Validation errors",
                                    value = """
                                            {
                                              "error": "Order product number must be greater or equal 100 000"
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
                    description = "Order product not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Not found example",
                                    value = """
                                            {
                                              "error": "Order product not found"
                                            }
                                            """
                            )
                    )
            )
    })
    @PutMapping("/product")
    @PreAuthorize("hasAnyAuthority('orders:write')")
    public ResponseEntity<?> updateOrderProduct(
            @Parameter(
                    description = "UUID of the order product to update",
                    required = true,
                    example = "30e9276f-ccce-45a7-9c28-e1ce22254eea"
            )
            @RequestParam UUID id,
            @RequestBody OrderProduct orderProduct) {
        return orderService.updateOrderProduct(id, orderProduct);
    }
}