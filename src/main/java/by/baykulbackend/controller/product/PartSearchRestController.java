package by.baykulbackend.controller.product;

import by.baykulbackend.database.dao.product.Part;
import by.baykulbackend.database.dto.security.Views;
import by.baykulbackend.database.repository.product.IPartRepository;
import by.baykulbackend.exceptions.NotFoundException;
import by.baykulbackend.services.product.PartService;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/product/search")
@RequiredArgsConstructor
@Tag(name = "Product Search", description = "API for spare part search operations")
public class PartSearchRestController {
    private final IPartRepository iPartRepository;
    private final PartService partService;

    @Operation(
            summary = "Search parts",
            description = "Searches parts by article or name containing the specified text (case-insensitive) with pagination. " +
                    "Requires products:read permission.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Parameters({
            @Parameter(name = "text", description = "Text to search for in article or name", required = true, example = "engine"),
            @Parameter(name = "page", description = "Page number (0-based, default: 0)", example = "0"),
            @Parameter(name = "size", description = "Page size (default: 50)", example = "50"),
            @Parameter(name = "sort", description = "Sort property and direction (e.g., createdTs,desc)", example = "createdTs,desc")
    })
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Search results retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = Views.PartView.Get.class)),
                            examples = @ExampleObject(
                                    name = "Search results example",
                                    summary = "Search results",
                                    value = """
                                            [
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
                                              },
                                              {
                                                "id": "123e4567-e89b-12d3-a456-426614174002",
                                                "createdTs": "2024-01-16T09:15:00",
                                                "updatedTs": "2024-01-19T11:20:00",
                                                "article": "2405948",
                                                "name": "Engine Oil Filter",
                                                "weight": 0.8,
                                                "minCount": 2,
                                                "storageCount": 8,
                                                "returnPart": 0.50,
                                                "price": 1200.00,
                                                "currency": "EUR",
                                                "brand": "BMW"
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
                            mediaType = "application/json",
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
                            mediaType = "application/json",
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
    @JsonView(Views.PartView.Get.class)
    public List<Part> search(
            @RequestParam(required = false, defaultValue = "") String text,
            @PageableDefault(size = 50, sort = "createdTs", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return partService.searchPart(text, pageable).stream().toList();
    }

    @Operation(
            summary = "Search parts by multiple filters",
            description = "Searches parts by article, name, and brand with pagination. All parameters are optional. Requires products:read permission.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Parameters({
            @Parameter(name = "article", description = "Article text to search for (case-insensitive, partial match)", example = "24059"),
            @Parameter(name = "name", description = "Name text to search for (case-insensitive, partial match)", example = "engine"),
            @Parameter(name = "brand", description = "Brand text to search for (case-insensitive, partial match)", example = "rolls"),
            @Parameter(name = "page", description = "Page number (0-based, default: 0)", example = "0"),
            @Parameter(name = "size", description = "Page size (default: 50)", example = "50"),
            @Parameter(name = "sort", description = "Sort property and direction (e.g., createdTs,desc)", example = "createdTs,desc")
    })
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Search results retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = Views.PartView.Get.class)),
                            examples = @ExampleObject(
                                    name = "Filter search results example",
                                    summary = "Filter search results",
                                    value = """
                                            [
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
                                            ]
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - JWT token missing or invalid",
                    content = @Content(
                            mediaType = "application/json",
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
                            mediaType = "application/json",
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
    @GetMapping("/filter")
    @JsonView(Views.PartView.Get.class)
    public List<Part> searchByFilter(
            @RequestParam(required = false) String article,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String brand,
            @PageableDefault(size = 50, sort = "createdTs", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return partService.searchPartsByFilter(article, name, brand, pageable).stream().toList();
    }

    @Operation(
            summary = "Get part by exact article",
            description = "Retrieves a specific part by exact article number. Requires products:read permission.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Part retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
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
                            mediaType = "application/json",
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
                            mediaType = "application/json",
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
                            mediaType = "application/json",
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
    @GetMapping("/exact/article")
    @JsonView(Views.PartView.Get.class)
    public Part getByArticle(
            @Parameter(
                    description = "Article number to search for",
                    required = true,
                    example = "2405947"
            )
            @RequestParam String article) {
        return iPartRepository.findByArticle(article).orElseThrow(() -> new NotFoundException("Part not found"));
    }

    @Operation(
            summary = "Search parts by article",
            description = "Searches parts by article containing the specified text (case-insensitive) with pagination. Requires products:read permission.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Parameters({
            @Parameter(name = "article", description = "Article text to search for (case-insensitive)", required = true, example = "24059"),
            @Parameter(name = "page", description = "Page number (0-based, default: 0)", example = "0"),
            @Parameter(name = "size", description = "Page size (default: 50)", example = "50"),
            @Parameter(name = "sort", description = "Sort property and direction (e.g., createdTs,desc)", example = "createdTs,desc")
    })
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Search results retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = Views.PartView.Get.class)),
                            examples = @ExampleObject(
                                    name = "Search by article results example",
                                    summary = "Search by article results",
                                    value = """
                                            [
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
                                              },
                                              {
                                                "id": "123e4567-e89b-12d3-a456-426614174003",
                                                "createdTs": "2024-01-17T14:20:00",
                                                "updatedTs": "2024-01-18T16:30:00",
                                                "article": "2405949",
                                                "name": "Brake Pads",
                                                "weight": 2.5,
                                                "minCount": 4,
                                                "storageCount": 12,
                                                "returnPart": 1.50,
                                                "price": 3500.00,
                                                "currency": "EUR",
                                                "brand": "Mercedes"
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
                            mediaType = "application/json",
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
                            mediaType = "application/json",
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
    @GetMapping("/article")
    @JsonView(Views.PartView.Get.class)
    public List<Part> searchByArticle(
            @RequestParam String article,
            @PageableDefault(size = 50, sort = "createdTs", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return iPartRepository.findByArticleContainingIgnoreCase(article, pageable).stream().toList();
    }

    @Operation(
            summary = "Get parts by exact name",
            description = "Retrieves parts by exact name with pagination. Requires products:read permission.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Parameters({
            @Parameter(name = "name", description = "Exact name to search for", required = true, example = "Engine Oil LL01 5W30"),
            @Parameter(name = "page", description = "Page number (0-based, default: 0)", example = "0"),
            @Parameter(name = "size", description = "Page size (default: 50)", example = "50"),
            @Parameter(name = "sort", description = "Sort property and direction (e.g., createdTs,desc)", example = "createdTs,desc")
    })
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Parts retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = Views.PartView.Get.class)),
                            examples = @ExampleObject(
                                    name = "Search by exact name results example",
                                    summary = "Search by exact name results",
                                    value = """
                                            [
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
                                            ]
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - JWT token missing or invalid",
                    content = @Content(
                            mediaType = "application/json",
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
                            mediaType = "application/json",
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
    @GetMapping("/exact/name")
    @JsonView(Views.PartView.Get.class)
    public List<Part> getByName(
            @RequestParam String name,
            @PageableDefault(size = 50, sort = "createdTs", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return iPartRepository.findByName(name, pageable).stream().toList();
    }

    @Operation(
            summary = "Search parts by name",
            description = "Searches parts by name containing the specified text (case-insensitive) with pagination. Requires products:read permission.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Parameters({
            @Parameter(name = "name", description = "Name text to search for (case-insensitive)", required = true, example = "engine oil"),
            @Parameter(name = "page", description = "Page number (0-based, default: 0)", example = "0"),
            @Parameter(name = "size", description = "Page size (default: 50)", example = "50"),
            @Parameter(name = "sort", description = "Sort property and direction (e.g., createdTs,desc)", example = "createdTs,desc")
    })
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Search results retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = Views.PartView.Get.class)),
                            examples = @ExampleObject(
                                    name = "Search by name results example",
                                    summary = "Search by name results",
                                    value = """
                                            [
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
                                              },
                                              {
                                                "id": "123e4567-e89b-12d3-a456-426614174002",
                                                "createdTs": "2024-01-16T09:15:00",
                                                "updatedTs": "2024-01-19T11:20:00",
                                                "article": "2405948",
                                                "name": "Engine Oil Filter",
                                                "weight": 0.8,
                                                "minCount": 2,
                                                "storageCount": 8,
                                                "returnPart": 0.50,
                                                "price": 1200.00,
                                                "currency": "EUR",
                                                "brand": "BMW"
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
                            mediaType = "application/json",
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
                            mediaType = "application/json",
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
    @GetMapping("/name")
    @JsonView(Views.PartView.Get.class)
    public List<Part> searchByName(
            @RequestParam String name,
            @PageableDefault(size = 50, sort = "createdTs", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return iPartRepository.findByNameContainingIgnoreCase(name, pageable).stream().toList();
    }

    @Operation(
            summary = "Get parts by exact brand",
            description = "Retrieves parts by exact brand name with pagination. Requires products:read permission.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Parameters({
            @Parameter(name = "brand", description = "Exact brand name to search for", required = true, example = "rolls royce"),
            @Parameter(name = "page", description = "Page number (0-based, default: 0)", example = "0"),
            @Parameter(name = "size", description = "Page size (default: 50)", example = "50"),
            @Parameter(name = "sort", description = "Sort property and direction (e.g., createdTs,desc)", example = "createdTs,desc")
    })
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Parts retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = Views.PartView.Get.class)),
                            examples = @ExampleObject(
                                    name = "Search by exact brand results example",
                                    summary = "Search by exact brand results",
                                    value = """
                                            [
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
                                              },
                                              {
                                                "id": "123e4567-e89b-12d3-a456-426614174005",
                                                "createdTs": "2024-01-18T13:10:00",
                                                "updatedTs": "2024-01-19T15:40:00",
                                                "article": "2405951",
                                                "name": "Spark Plug",
                                                "weight": 0.1,
                                                "minCount": 10,
                                                "storageCount": 50,
                                                "returnPart": 0.10,
                                                "price": 800.00,
                                                "currency": "EUR",
                                                "brand": "rolls royce"
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
                            mediaType = "application/json",
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
                            mediaType = "application/json",
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
    @GetMapping("/exact/brand")
    @JsonView(Views.PartView.Get.class)
    public List<Part> getByBrand(
            @RequestParam String brand,
            @PageableDefault(size = 50, sort = "createdTs", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return iPartRepository.findByBrand(brand, pageable).stream().toList();
    }

    @Operation(
            summary = "Search parts by brand",
            description = "Searches parts by brand containing the specified text (case-insensitive) with pagination. Requires products:read permission.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Parameters({
            @Parameter(name = "brand", description = "Brand text to search for (case-insensitive)", required = true, example = "roll"),
            @Parameter(name = "page", description = "Page number (0-based, default: 0)", example = "0"),
            @Parameter(name = "size", description = "Page size (default: 50)", example = "50"),
            @Parameter(name = "sort", description = "Sort property and direction (e.g., createdTs,desc)", example = "createdTs,desc")
    })
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Search results retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = Views.PartView.Get.class)),
                            examples = @ExampleObject(
                                    name = "Search by brand results example",
                                    summary = "Search by brand results",
                                    value = """
                                            [
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
                                              },
                                              {
                                                "id": "123e4567-e89b-12d3-a456-426614174002",
                                                "createdTs": "2024-01-16T09:15:00",
                                                "updatedTs": "2024-01-19T11:20:00",
                                                "article": "2405948",
                                                "name": "Engine Oil Filter",
                                                "weight": 0.8,
                                                "minCount": 2,
                                                "storageCount": 8,
                                                "returnPart": 0.50,
                                                "price": 1200.00,
                                                "currency": "EUR",
                                                "brand": "BMW"
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
                            mediaType = "application/json",
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
                            mediaType = "application/json",
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
    @GetMapping("/brand")
    @JsonView(Views.PartView.Get.class)
    public List<Part> searchByBrand(
            @RequestParam String brand,
            @PageableDefault(size = 50, sort = "createdTs", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return iPartRepository.findByBrandContainingIgnoreCase(brand, pageable).stream().toList();
    }
}