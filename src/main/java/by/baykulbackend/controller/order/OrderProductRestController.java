package by.baykulbackend.controller.order;

import by.baykulbackend.database.dao.order.BoxStatus;
import by.baykulbackend.database.dao.order.OrderProduct;
import by.baykulbackend.database.dto.security.Views;
import by.baykulbackend.database.repository.order.IOrderProductRepository;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/order/product")
@RequiredArgsConstructor
@Tag(name = "Order products", description = "Order products management")
public class OrderProductRestController {
    private final OrderService orderService;
    private final IOrderProductRepository iOrderProductRepository;

    @Operation(
            summary = "Search order products",
            description = "Search for order products by number or status. If number is provided, status is ignored. " +
                    "If no parameters are provided, returns all order products. " +
                    "Requires orders:read or all-orders:read permission.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Parameters({
            @Parameter(name = "number", description = "Order product number to search for (exact match)", example = "100001"),
            @Parameter(name = "status", description = "Order product status to filter by", example = "IN_WAREHOUSE"),
            @Parameter(name = "forBill", description = "To get only products, that require bill", example = "true"),
            @Parameter(name = "page", description = "Page number (0-based, default: 0)", example = "0"),
            @Parameter(name = "size", description = "Page size (default: 50)", example = "50"),
            @Parameter(name = "sort", description = "Sort property and direction (e.g., createdTs,desc)", example = "createdTs,desc")
    })
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "List of order products retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = Views.OrderProductFullView.class))
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
    @GetMapping("/search")
    @PreAuthorize("hasAnyAuthority('all-orders:read')")
    @JsonView(Views.OrderProductFullView.class)
    public List<OrderProduct> search(
            @Parameter(hidden = true) @RequestParam(required = false) Long number,
            @Parameter(hidden = true) @RequestParam(required = false) BoxStatus status,
            @Parameter(hidden = true) @RequestParam(required = false) Boolean forBill,
            @PageableDefault(size = 50, sort = "createdTs", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return orderService.searchOrderProducts(number, status, forBill, pageable).stream().toList();
    }

    @Operation(
            summary = "Update order product",
            description = """
                    Updates an order product. Requires all-orders:write permission.
                    
                    **Available status transitions for BoxStatus:**
                    - From CREATED → CANCELLED
                    - From TO_ORDER → ON_WAY
                    - From ARRIVED, IN_WAREHOUSE → SHIPPED
                    - From SHIPPED → DELIVERED (id order is paid)
                    - From DELIVERED → RETURNED
                    
                    The following fields can be updated:
                    - `status` - must follow the transition rules above
                    - `number` - order product number (must be >= 100000)
                    - `partsCount` - quantity of parts (must be >= 1)
                    """,
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
                                              "status": "SHIPPED",
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
                    description = """
                            Bad request - invalid order product data. Possible reasons:
                            - Status transition not allowed from current state
                            - Order product number must be >= 100000
                            - Parts count must be >= 1
                            """,
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
    @PatchMapping
    @PreAuthorize("hasAnyAuthority('all-orders:write')")
    public ResponseEntity<?> update(
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
