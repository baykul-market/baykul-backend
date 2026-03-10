package by.baykulbackend.controller.finance;

import by.baykulbackend.database.dto.finance.PriceConfigDto;
import by.baykulbackend.database.dto.security.Views;
import by.baykulbackend.services.finance.PriceService;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/price-config")
@RequiredArgsConstructor
@Tag(name = "Price Configuration", description = "API for managing global price configuration settings")
public class PriceConfigController {
    private final PriceService priceService;

    @Operation(
            summary = "Get price configuration",
            description = "Retrieves the global price configuration with delivery percentage, markup percentage and currency. " +
                    "Requires pricing:read permission.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Configuration retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PriceConfigDto.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "deliveryPercentage": 0.10,
                                              "markupPercentage": 0.10,
                                              "currency": "RUB"
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
                    description = "Forbidden - insufficient permissions"
            )
    })
    @PreAuthorize("hasAnyAuthority('pricing:read')")
    @GetMapping
    @JsonView(Views.PriceConfigView.Get.class)
    public PriceConfigDto getConfig() {
        return priceService.getConfig();
    }

    @Operation(
            summary = "Update price configuration",
            description = "Updates the global price configuration. All fields are optional - only provided fields will be updated. " +
                    "If configuration doesn't exist, creates new one with defaults for missing fields. " +
                    "Requires pricing:write permission.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Configuration updated successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "update_price_config": "true"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request - invalid configuration data",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "Invalid delivery percentage",
                                            value = """
                                                    {
                                                      "error_delivery_percentage": "Delivery percentage must be greater than or equal to zero"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Invalid markup percentage",
                                            value = """
                                                    {
                                                      "error_markup_percentage": "Markup percentage must be greater than or equal to zero"
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - JWT token missing or invalid"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - insufficient permissions"
            )
    })
    @PreAuthorize("hasAnyAuthority('pricing:write')")
    @PutMapping
    public ResponseEntity<?> updateConfig(@RequestBody PriceConfigDto configDto) {
        return priceService.updateConfig(configDto);
    }

    @Operation(
            summary = "Reset price configuration to default",
            description = "Resets the global price configuration to default values (10% delivery, 10% markup, RUB currency). " +
                    "Requires pricing:write permission.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Configuration reset successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "reset_price_config": "true"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - JWT token missing or invalid"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - insufficient permissions"
            )
    })
    @PreAuthorize("hasAnyAuthority('pricing:write')")
    @PostMapping("/reset")
    public ResponseEntity<?> resetConfig() {
        return priceService.resetToDefault();
    }
}