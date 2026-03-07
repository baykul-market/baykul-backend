package by.baykulbackend.controller.bill;

import by.baykulbackend.database.dao.bill.Bill;
import by.baykulbackend.database.dto.security.Views;
import by.baykulbackend.database.repository.bill.IBillRepository;
import by.baykulbackend.exceptions.NotFoundException;
import by.baykulbackend.services.bill.BillService;
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
@RequestMapping("/api/v1/bill")
@RequiredArgsConstructor
@Tag(name = "Bills", description = "Bills management endpoints for creating and managing bills of order products")
public class BillRestController {
    private final BillService billService;
    private final IBillRepository iBillRepository;

    @Operation(
            summary = "Get all bills",
            description = "Retrieves all bills with pagination support. Requires all-bills:read permission.",
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
                    description = "Bills retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Views.BillView.Get.class),
                            examples = @ExampleObject(
                                    name = "Bills response example",
                                    summary = "List of bills",
                                    value = """
                                            [
                                              {
                                                "id": "123e4567-e89b-12d3-a456-426614174000",
                                                "createdTs": "2024-01-15T10:30:00",
                                                "updatedTs": "2024-01-20T14:45:30",
                                                "number": 10001,
                                                "status": "DRAFT"
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
    @PreAuthorize("hasAnyAuthority('all-bills:read')")
    @JsonView(Views.BillView.Get.class)
    public List<Bill> getAll(
            @PageableDefault(size = 50, sort = "createdTs", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return iBillRepository.findAll(pageable).stream().toList();
    }

    @Operation(
            summary = "Get bill by ID",
            description = "Retrieves a specific bill by its UUID with full details. Requires all-bills:read permission.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Bill retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Bill.class),
                            examples = @ExampleObject(
                                    name = "Single bill response example",
                                    summary = "Bill details",
                                    value = """
                                            {
                                              "id": "123e4567-e89b-12d3-a456-426614174000",
                                              "createdTs": "2024-01-15T10:30:00",
                                              "updatedTs": "2024-01-20T14:45:30",
                                              "number": 10001,
                                              "status": "DRAFT",
                                              "orderProducts": [
                                                {
                                                  "id": "30e9276f-ccce-45a7-9c28-e1ce22254eea",
                                                  "number": 100001,
                                                  "status": "ORDERED",
                                                  "partsCount": 3,
                                                  "price": 23587.29,
                                                  "currency": "EUR",
                                                  "part": {
                                                    "id": "63e9276f-ccce-45a7-9c28-e1ce24354eea",
                                                    "article": "2405947",
                                                    "name": "Engine Oil LL01 5W30"
                                                  }
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
                    description = "Bill not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Not found example",
                                    value = """
                                            {
                                              "error": "Bill not found"
                                            }
                                            """
                            )
                    )
            )
    })
    @GetMapping("/id")
    @PreAuthorize("hasAnyAuthority('all-bills:read')")
    @JsonView(Views.BillFullView.class)
    public Bill getOne(
            @Parameter(
                    description = "UUID of the bill to retrieve",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000"
            )
            @RequestParam UUID id
    ) {
        return iBillRepository.findById(id).orElseThrow(() -> new NotFoundException("Bill not found"));
    }

    @Operation(
            summary = "Create a new bill",
            description = "Creates a new bill in DRAFT status. Can optionally include order products. " +
                    "Requires all-bills:write permission.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Bill created successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Create bill success example",
                                    summary = "Bill created successfully",
                                    value = """
                                            {
                                              "create_bill": "true",
                                              "id": "123e4567-e89b-12d3-a456-426614174000"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request - some order products are unavailable",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Bad request example",
                                    value = """
                                            {
                                              "unavailable_order_products": [
                                                "123e4567-e89b-12d3-a456-426614174001"
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
            )
    })
    @PostMapping("/create")
    @PreAuthorize("hasAnyAuthority('all-bills:write')")
    public ResponseEntity<?> create(
            @JsonView(Views.BillCreateFullView.class)
            @RequestBody
            Bill bill
    ) {
        return billService.createBill(bill);
    }

    @Operation(
            summary = "Apply a bill",
            description = "Changes bill status from DRAFT to APPLIED. Only draft bills can be applied. " +
                    "Requires all-bills:write permission.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Bill applied successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Apply bill success example",
                                    value = """
                                            {
                                              "update_bill": "true"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request - bill is not in DRAFT status",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Bad request example",
                                    value = """
                                            {
                                              "error": "Cannot update non-draft bill"
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
                    description = "Bill not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Not found example",
                                    value = """
                                            {
                                              "error": "Bill not found"
                                            }
                                            """
                            )
                    )
            )
    })
    @PostMapping("/apply")
    @PreAuthorize("hasAnyAuthority('all-bills:write')")
    public ResponseEntity<?> apply(
            @Parameter(
                    description = "UUID of the bill to apply",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000"
            )
            @RequestParam UUID id
    ) {
        return billService.applyBill(id);
    }

    @Operation(
            summary = "Add order product to bill",
            description = "Adds an order product to an existing draft bill. " +
                    "Requires all-bills:write permission.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Order product added to bill successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Add to bill success example",
                                    value = """
                                            {
                                              "update_bill": "true"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request - bill not in DRAFT status or order product already in another applied bill",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Bad request example",
                                    value = """
                                            {
                                              "error": "Order product is already in bill 123e4567-e89b-12d3-a456-426614174001"
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
                    description = "Bill or order product not found",
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
    @PostMapping("/add")
    @PreAuthorize("hasAnyAuthority('all-bills:write')")
    public ResponseEntity<?> add(
            @Parameter(
                    description = "UUID of the order product to add to bill",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000"
            )
            @RequestParam UUID orderProductId,
            @Parameter(
                    description = "UUID of the bill",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000"
            )
            @RequestParam UUID billId
    ) {
        return billService.addBoxToBill(billId, orderProductId);
    }

    @Operation(
            summary = "Remove order product from bill",
            description = "Removes an order product from a draft bill. " +
                    "Requires all-bills:write permission.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Order product removed from bill successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Remove from bill success example",
                                    value = """
                                            {
                                              "update_bill": "true"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request - bill not in DRAFT status or order product not in this bill",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Bad request example",
                                    value = """
                                            {
                                              "error": "Order product is not in that bill"
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
                    description = "Bill or order product not found",
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
    @PostMapping("/remove")
    @PreAuthorize("hasAnyAuthority('all-bills:write')")
    public ResponseEntity<?> remove(
            @Parameter(
                    description = "UUID of the order product to remove from bill",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000"
            )
            @RequestParam UUID orderProductId,
            @Parameter(
                    description = "UUID of the bill",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000"
            )
            @RequestParam UUID billId
    ) {
        return billService.removeBoxFromBill(billId, orderProductId);
    }

    @Operation(
            summary = "Delete a bill",
            description = "Deletes a draft bill and detaches all associated order products. " +
                    "Only draft bills can be deleted. Requires all-bills:write permission.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Bill deleted successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Delete bill success example",
                                    value = """
                                            {
                                              "delete_bill": "true"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request - bill is not in DRAFT status",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Bad request example",
                                    value = """
                                            {
                                              "error": "Can delete only draft bill"
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
                    description = "Bill not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Not found example",
                                    value = """
                                            {
                                              "error": "Bill not found"
                                            }
                                            """
                            )
                    )
            )
    })
    @DeleteMapping
    @PreAuthorize("hasAnyAuthority('all-bills:write')")
    public ResponseEntity<?> delete(
            @Parameter(
                    description = "UUID of the bill to delete",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000"
            )
            @RequestParam UUID id
    ) {
        return billService.deleteBillById(id);
    }
}