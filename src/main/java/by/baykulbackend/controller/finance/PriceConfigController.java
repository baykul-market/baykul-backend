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
import org.springframework.web.bind.annotation.*;

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
                                              "update_base_config": "true"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Bad request - invalid data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @PreAuthorize("hasAnyAuthority('pricing:write')")
    @PutMapping("/base")
    public ResponseEntity<?> updateBaseConfig(@RequestBody PriceConfigDto configDto) {
        return priceService.updateBaseConfig(configDto);
    }

    @Operation(
            summary = "Create or update delivery cost rule",
            description = "Creates new or updates existing delivery cost rule. " +
                    "For SUM type, currency is required. Requires pricing:write permission.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Rule saved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "save_delivery_rule": "true",
                                              "id": "123e4567-e89b-12d3-a456-426614174003"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Bad request - invalid rule data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Rule not found")
    })
    @PreAuthorize("hasAnyAuthority('pricing:write')")
    @PostMapping("/delivery-rule")
    public ResponseEntity<?> saveDeliveryRule(@RequestBody DeliveryCostConfigDto dto) {
        return priceService.saveDeliveryCostRule(dto);
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
            @ApiResponse(responseCode = "404", description = "Rule not found")
    })
    @PreAuthorize("hasAnyAuthority('pricing:write')")
    @DeleteMapping("/delivery-rule")
    public ResponseEntity<?> deleteDeliveryRule(@Parameter(description = "UUID of delivery rule") @RequestParam UUID id) {
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