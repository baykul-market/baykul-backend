package by.baykulbackend.controller.product;

import by.baykulbackend.database.dao.product.Part;
import by.baykulbackend.database.dto.product.PartDto;
import by.baykulbackend.database.dto.product.ProductDto;
import by.baykulbackend.database.dto.security.Views;
import by.baykulbackend.database.repository.product.IPartRepository;
import by.baykulbackend.services.product.PartService;
import by.baykulbackend.services.product.ProductCsvService;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameters;
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
@RequestMapping("/api/v1/product")
@RequiredArgsConstructor
@Tag(name = "Product Management", description = "API for spare part management operations")
public class PartRestController {
    private final ProductCsvService productCsvService;
    private final IPartRepository iPartRepository;
    private final PartService partService;

    @Operation(
            summary = "Get all parts",
            description = "Retrieves all spare parts from the system with pagination support. " +
                    "Requires products:read permission.",
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
                    description = "Page of parts retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PartDto.class),
                            examples = @ExampleObject(
                                    name = "All parts response example",
                                    summary = "List of all spare parts",
                                    value = """
                                            {
                                              "content": [
                                                {
                                                  "id": "123e4567-e89b-12d3-a456-426614174001",
                                                  "createdTs": "2024-01-15T10:30:00",
                                                  "updatedTs": "2024-01-20T14:45:30",
                                                  "article": "2405947",
                                                  "name": "Engine Oil LL01 5W30",
                                                  "weight": 150.4,
                                                  "minCount": 3,
                                                  "storageCount": 5,
                                                  "returnPart": 3.01,
                                                  "price": 7862.43,
                                                  "currency": "EUR",
                                                  "brand": "rolls royce"
                                                }
                                              ],
                                              "pageable": {
                                                "pageNumber": 0,
                                                "pageSize": 20,
                                                "sort": {
                                                  "sorted": true,
                                                  "unsorted": false
                                                }
                                              },
                                              "totalPages": 5,
                                              "totalElements": 100,
                                              "last": false,
                                              "first": true,
                                              "empty": false
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
    @PreAuthorize("hasAnyAuthority('products:read')")
    @GetMapping
    public List<PartDto> getAll(
            @PageableDefault(size = 50, sort = "createdTs", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return partService.getAllParts(pageable).stream().toList();
    }

    @Operation(
            summary = "Get part by ID",
            description = "Retrieves a specific spare part by UUID. Requires products:read permission.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Part retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Views.PartView.Get.class),
                            examples = @ExampleObject(
                                    name = "Single part response example",
                                    summary = "Part details",
                                    value = """
                                            {
                                              "id": "123e4567-e89b-12d3-a456-426614174001",
                                              "createdTs": "2024-01-15T10:30:00",
                                              "updatedTs": "2024-01-20T14:45:30",
                                              "article": "2405947",
                                              "name": "Engine Oil LL01 5W30",
                                              "weight": 150.4,
                                              "minCount": 3,
                                              "storageCount": 5,
                                              "returnPart": 3.01,
                                              "price": 7862.43,
                                              "currency": "EUR",
                                              "brand": "rolls royce"
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
                    description = "Part not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Not found example",
                                    value = """
                                            {
                                              "error": "Part not found"
                                            }
                                            """
                            )
                    )
            )
    })
    @PreAuthorize("hasAnyAuthority('products:read')")
    @GetMapping("/id")
    public PartDto getOne(
            @Parameter(
                    description = "UUID of the part to retrieve",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174001"
            )
            @RequestParam UUID id) {
        return partService.getPartById(id);
    }

    @Operation(
            summary = "Create new part",
            description = "Creates a new spare part in the system. Requires products:write permission.",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Part data to create",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = Views.PartView.Post.class),
                            examples = @ExampleObject(
                                    name = "Create part request example",
                                    summary = "Part creation request",
                                    value = """
                                            {
                                              "article": "2405947",
                                              "name": "Engine Oil LL01 5W30",
                                              "weight": 150.4,
                                              "minCount": 3,
                                              "storageCount": 5,
                                              "returnPart": 3.01,
                                              "price": 7862.43,
                                              "currency": "EUR",
                                              "brand": "rolls royce"
                                            }
                                            """
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Part created successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Create part success example",
                                    summary = "Part created successfully",
                                    value = """
                                            {
                                              "create_part": "true",
                                              "id": "123e4567-e89b-12d3-a456-426614174003"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request - invalid part data",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Validation error example",
                                    summary = "Validation errors",
                                    value = """
                                            {
                                              "error_article": "The article must not be empty",
                                              "error_name": "The name must not be empty",
                                              "error_brand": "The brand must not be empty"
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
                    description = "Conflict - part with same article already exists",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Conflict example",
                                    summary = "Duplicate part data",
                                    value = """
                                            {
                                              "error_article": "Part with that article already exists"
                                            }
                                            """
                            )
                    )
            )
    })
    @PreAuthorize("hasAnyAuthority('products:write')")
    @PostMapping
    public ResponseEntity<?> create(@RequestBody @JsonView(Views.PartView.Post.class) Part part) {
        return partService.createPart(part);
    }

    @Operation(
            summary = "Upload parts from CSV file",
            description = "Uploads and parses multiple spare parts from a CSV file. " +
                    "File must be in UTF-8 encoding with semicolon (;) separator. Requires products:write permission.",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "CSV file containing parts data",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(implementation = ProductDto.class),
                            examples = @ExampleObject(
                                    name = "CSV upload request example",
                                    summary = "CSV file upload",
                                    value = """
                                            The CSV file should have the following format:
                                            Header: article;name;weight;min_count;storage_count;return_part;price;brand
                                            Example row: 2405947;Engine Oil LL01 5W30;150.4;3;5;3.01;7862.43;rolls royce
                                            """
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "CSV file parsed and parts created successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Upload success example",
                                    summary = "Upload successful",
                                    value = """
                                            {
                                              "parsed": "true"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request - invalid CSV format or data",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "CSV validation error example",
                                    summary = "CSV parsing errors",
                                    value = """
                                            {
                                              "error": "Error while parsing csv file",
                                              "error_row_2": "Incorrect number of columns",
                                              "error_row_3": "Duplicate article 2405947",
                                              "error_row_4": "Incorrect name size"
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
    @PreAuthorize("hasAnyAuthority('products:write')")
    @PostMapping(value = "/upload", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<?> uploadParts(
            @Parameter(
                    description = "CSV file with parts data",
                    required = true
            )
            ProductDto productDto) {
        return productCsvService.parseParts(productDto);
    }

    @Operation(
            summary = "Update part",
            description = "Updates an existing part's information. Only non-null fields are updated. Requires products:write permission.",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Part data to update",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = Views.PartView.Patch.class),
                            examples = @ExampleObject(
                                    name = "Update part request example",
                                    summary = "Part update request",
                                    value = """
                                            {
                                              "article": "2405948",
                                              "name": "Updated Engine Oil",
                                              "weight": 155.0,
                                              "minCount": 4,
                                              "storageCount": 8,
                                              "returnPart": 4.01,
                                              "price": 8000.00,
                                              "currency": "USD",
                                              "brand": "Updated Brand"
                                            }
                                            """
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Part updated successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Update part success example",
                                    summary = "Part updated successfully",
                                    value = """
                                            {
                                              "update_part": "true"
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
                    description = "Part not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Not found example",
                                    value = """
                                            {
                                              "error": "Part not found",
                                              "message": "Part with id 123e4567-e89b-12d3-a456-426614174001 not found"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflict - part with same article already exists",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Conflict example",
                                    summary = "Duplicate part data",
                                    value = """
                                            {
                                              "error_article": "Part with that article already exists"
                                            }
                                            """
                            )
                    )
            )
    })
    @Transactional
    @PreAuthorize("hasAnyAuthority('products:write')")
    @PatchMapping
    public ResponseEntity<?> update(
            @Parameter(
                    description = "UUID of the part to update",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174001"
            )
            @RequestParam UUID id,
            @RequestBody @JsonView(Views.PartView.Patch.class) Part part) {
        return partService.updatePart(id, part);
    }

    @Operation(
            summary = "Delete part",
            description = "Deletes a part by ID. Requires products:write permission.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Part deleted successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Delete part success example",
                                    summary = "Part deleted successfully",
                                    value = """
                                            {
                                              "delete_part": "true"
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
                    description = "Part not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Not found example",
                                    value = """
                                            {
                                              "error": "Part not found",
                                              "message": "Part with id 123e4567-e89b-12d3-a456-426614174001 not found"
                                            }
                                            """
                            )
                    )
            )
    })
    @PreAuthorize("hasAnyAuthority('products:write')")
    @DeleteMapping
    public ResponseEntity<?> delete(
            @Parameter(
                    description = "UUID of the part to delete",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174001"
            )
            @RequestParam UUID id) {
        return partService.deletePartById(id);
    }
}