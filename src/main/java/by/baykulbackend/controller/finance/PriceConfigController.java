package by.baykulbackend.controller.finance;

import by.baykulbackend.database.dto.finance.DeliveryCostConfigDto;
import by.baykulbackend.database.dto.finance.PriceConfigDto;
import by.baykulbackend.services.finance.PriceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/price-config")
@RequiredArgsConstructor
@Tag(name = "Price Configuration", description = "API for managing global price configuration settings")
public class PriceConfigController {
    private final PriceService priceService;

    @Operation(
            summary = "Get all configurations",
            description = "Retrieves markup percentage, system currency and delivery cost rules. Requires pricing:read permission.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Configurations retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PriceConfigDto.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "markupPercentage": 0.10,
                                              "systemCurrency": "RUB",
                                              "deliveryCostConfigs": [
                                                {
                                                  "id": "123e4567-e89b-12d3-a456-426614174001",
                                                  "minimumSum": 0,
                                                  "markupType": "PERCENTAGE",
                                                  "value": 0.10
                                                }
                                              ]
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @PreAuthorize("hasAnyAuthority('pricing:read')")
    @GetMapping
    public PriceConfigDto getAllConfigs() {
        return priceService.getAllConfigs();
    }

    @Operation(
            summary = "Update base configuration",
            description = "Updates markup percentage and/or system currency. All fields are optional. " +
                    "Requires pricing:write permission.",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Base configuration data to update",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PriceConfigDto.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Update both fields",
                                            summary = "Update markup percentage and currency",
                                            value = """
                                                    {
                                                      "markupPercentage": 0.15,
                                                      "systemCurrency": "USD"
                                                    }
                                                    """
                                    )
                            }
                    )
            )
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
                                              "update_base_config": "true"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request - invalid data",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "error_markup": "Markup percentage must be greater than or equal to zero"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @PreAuthorize("hasAnyAuthority('pricing:write')")
    @PutMapping("/base")
    public ResponseEntity<?> updateBaseConfig(@RequestBody PriceConfigDto configDto) {
        return priceService.updateBaseConfig(configDto);
    }

    @Operation(
            summary = "Create delivery cost rule",
            description = "Creates a new delivery cost rule. " +
                    "For SUM type, currency is required. Requires pricing:write permission.",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Delivery cost rule data",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DeliveryCostConfigDto.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Percentage type rule",
                                            summary = "Create percentage-based rule",
                                            value = """
                                                    {
                                                      "minimumSum": 0,
                                                      "markupType": "PERCENTAGE",
                                                      "value": 0.10
                                                    }
                                                    """
                                    )
                            }
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Rule created successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "create_delivery_rule": "true",
                                              "id": "123e4567-e89b-12d3-a456-426614174003"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request - invalid rule data",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "Invalid minimum sum",
                                            value = """
                                                    {
                                                      "error": "Minimum sum must be greater than or equal to zero"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Invalid value",
                                            value = """
                                                    {
                                                      "error": "Value must be greater than or equal to zero"
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @PreAuthorize("hasAnyAuthority('pricing:write')")
    @PostMapping("/delivery-rule")
    public ResponseEntity<?> createDeliveryRule(@RequestBody DeliveryCostConfigDto dto) {
        return priceService.createDeliveryCostRule(dto);
    }

    @Operation(
            summary = "Update delivery cost rule",
            description = "Updates an existing delivery cost rule by ID. " +
                    "For SUM type, currency is required. Requires pricing:write permission.",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Delivery cost rule data to update",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DeliveryCostConfigDto.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Update percentage rule",
                                            summary = "Update existing percentage rule",
                                            value = """
                                                    {
                                                      "id": "123e4567-e89b-12d3-a456-426614174001",
                                                      "minimumSum": 0,
                                                      "markupType": "PERCENTAGE",
                                                      "value": 0.15
                                                    }
                                                    """
                                    )
                            }
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Rule updated successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "update_delivery_rule": "true",
                                              "id": "123e4567-e89b-12d3-a456-426614174001"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request - invalid rule data",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "error": "Value must be greater than or equal to zero"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Rule not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "error": "Delivery cost rule not found"
                                            }
                                            """
                            )
                    )
            )
    })
    @PreAuthorize("hasAnyAuthority('pricing:write')")
    @PutMapping("/delivery-rule")
    public ResponseEntity<?> updateDeliveryRule(@RequestBody DeliveryCostConfigDto dto) {
        return priceService.updateDeliveryCostRule(dto);
    }

    @Operation(
            summary = "Delete delivery cost rule",
            description = "Deletes a delivery cost rule by ID. Requires pricing:write permission.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Rule deleted successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "delete_delivery_rule": "true"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Rule not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "error": "Delivery cost rule not found"
                                            }
                                            """
                            )
                    )
            )
    })
    @PreAuthorize("hasAnyAuthority('pricing:write')")
    @DeleteMapping("/delivery-rule")
    public ResponseEntity<?> deleteDeliveryRule(
            @Parameter(description = "UUID of delivery rule", required = true, example = "123e4567-e89b-12d3-a456-426614174001")
            @RequestParam UUID id) {
        return priceService.deleteDeliveryCostRule(id);
    }

    @Operation(
            summary = "Reset all configurations to default",
            description = "Resets markup percentage, currency and delivery rules to default values. " +
                    "Requires pricing:write permission.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "All configurations reset successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "reset_all_configs": "true"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @PreAuthorize("hasAnyAuthority('pricing:write')")
    @PostMapping("/reset")
    public ResponseEntity<?> resetAllConfigs() {
        return priceService.resetAllToDefault();
    }
}