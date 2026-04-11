package by.baykulbackend.controller.finance;

import by.baykulbackend.database.dao.finance.CurrencyExchange;
import by.baykulbackend.database.dto.finance.CurrencyDto;
import by.baykulbackend.database.dto.finance.CurrencyExchangeDto;
import by.baykulbackend.database.dto.security.Views;
import by.baykulbackend.database.repository.finance.ICurrencyExchangeRepository;
import by.baykulbackend.services.finance.CurrencyExchangeService;
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
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/currency-exchange")
@RequiredArgsConstructor
@Tag(name = "Currency Exchange Management", description = "API for managing currency exchange rates")
public class CurrencyExchangeRestController {
    private final ICurrencyExchangeRepository iCurrencyExchangeRepository;
    private final CurrencyExchangeService currencyExchangeService;

    @Operation(
            summary = "Get all currency exchanges",
            description = "Retrieves all currency exchange rates from the system. " +
                    "Requires pricing:read permission.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "List of currency exchanges retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = Views.CurrencyExchangeView.Get.class)),
                            examples = @ExampleObject(
                                    name = "All currency exchanges response example",
                                    summary = "List of all currency exchange rates",
                                    value = """
                                            [
                                              {
                                                "id": "123e4567-e89b-12d3-a456-426614174001",
                                                "createdTs": "2024-01-15T10:30:00",
                                                "updatedTs": "2024-01-20T14:45:30",
                                                "currencyFrom": "USD",
                                                "currencyTo": "EUR",
                                                "rate": 0.85
                                              },
                                              {
                                                "id": "123e4567-e89b-12d3-a456-426614174002",
                                                "createdTs": "2024-01-16T09:15:00",
                                                "updatedTs": "2024-01-19T11:20:00",
                                                "currencyFrom": "EUR",
                                                "currencyTo": "USD",
                                                "rate": 1.18
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
    @PreAuthorize("hasAnyAuthority('pricing:read')")
    @GetMapping
    @JsonView(Views.CurrencyExchangeView.Get.class)
    public List<CurrencyExchange> getAll() {
        return iCurrencyExchangeRepository.findAll();
    }

    @Operation(
            summary = "Get currency exchange by ID",
            description = "Retrieves a specific currency exchange rate by UUID. " +
                    "Requires pricing:read permission.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Currency exchange retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Views.CurrencyExchangeView.Get.class),
                            examples = @ExampleObject(
                                    name = "Single currency exchange response example",
                                    summary = "Currency exchange details",
                                    value = """
                                            {
                                              "id": "123e4567-e89b-12d3-a456-426614174001",
                                              "createdTs": "2024-01-15T10:30:00",
                                              "updatedTs": "2024-01-20T14:45:30",
                                              "currencyFrom": "USD",
                                              "currencyTo": "EUR",
                                              "rate": 0.85
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
                    description = "Currency exchange not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Not found example",
                                    value = """
                                            {
                                              "error": "Currency exchange not found"
                                            }
                                            """
                            )
                    )
            )
    })
    @PreAuthorize("hasAnyAuthority('pricing:read')")
    @JsonView(Views.CurrencyExchangeView.Get.class)
    @GetMapping("/id")
    public CurrencyExchange getOne(
            @Parameter(
                    description = "UUID of the currency exchange to retrieve",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174001"
            )
            @RequestParam UUID id) {
        return iCurrencyExchangeRepository.findById(id)
                .orElseThrow(() -> new by.baykulbackend.exceptions.NotFoundException("Currency exchange not found"));
    }

    @Operation(
            summary = "Get all available currencies",
            description = "Retrieves list of all supported currencies with their details. " +
                    "Returns currency codes, Russian names, and countries of use. " +
                    "Requires pricing:read permission.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "List of currencies retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = CurrencyDto.class)),
                            examples = @ExampleObject(
                                    name = "Currencies list response example",
                                    summary = "List of all supported currencies",
                                    value = """
                                            [
                                              {
                                                "code": "USD",
                                                "russianName": "Доллар США",
                                                "countries": "США, Эквадор, Сальвадор, Панама и др."
                                              },
                                              {
                                                "code": "EUR",
                                                "russianName": "Евро",
                                                "countries": "Европейский союз, Андорра, Австрия, Бельгия, Хорватия и др."
                                              },
                                              {
                                                "code": "RUB",
                                                "russianName": "Российский рубль",
                                                "countries": "Россия"
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
    @PreAuthorize("hasAnyAuthority('pricing:read')")
    @GetMapping("/currency")
    public List<CurrencyDto> getCurrencies() {
        return currencyExchangeService.getAllCurrencies();
    }

    @Operation(
            summary = "Create or update currency exchange",
            description = "Creates a new currency exchange rate or updates an existing one. " +
                    "Can create exchange rate in both directions simultaneously. " +
                    "Requires pricing:write permission.",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Currency exchange data to create or update",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = CurrencyExchangeDto.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Create new exchange rate example",
                                            summary = "Create new exchange rate",
                                            value = """
                                                    {
                                                      "currencyFrom": "USD",
                                                      "currencyTo": "EUR",
                                                      "rate": 0.85,
                                                      "bothDirections": true,
                                                      "replaceExisting": true
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
                    description = "Currency exchange created/updated successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Success response example",
                                    summary = "Operation successful",
                                    value = """
                                            {
                                              "create_currency_exchange": "true"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request - invalid currency exchange data",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "Empty currency validation",
                                            summary = "Empty currency fields",
                                            value = """
                                                    {
                                                      "error_currency": "Currency values must not be empty"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Rate validation",
                                            summary = "Invalid rate value",
                                            value = """
                                                    {
                                                      "error_rate": "Rate value must be greater than zero"
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
                    responseCode = "409",
                    description = "Conflict - exchange rate already exists and replaceExisting is false",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "Existing exchange rate conflict",
                                            summary = "Rate already exists",
                                            value = """
                                                    {
                                                      "error": "Currency exchange already exists"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Reversed rate conflict",
                                            summary = "Reversed rate already exists",
                                            value = """
                                                    {
                                                      "error": "Reversed currency exchange already exists"
                                                    }
                                                    """
                                    )
                            }
                    )
            )
    })
    @PreAuthorize("hasAnyAuthority('pricing:write')")
    @PostMapping
    public ResponseEntity<?> createUpdate(@RequestBody CurrencyExchangeDto currencyExchangeDto) {
        return currencyExchangeService.createUpdateCurrencyExchange(currencyExchangeDto);
    }

    @Operation(
            summary = "Delete currency exchange",
            description = "Deletes a currency exchange rate by ID. " +
                    "Requires pricing:write permission.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Currency exchange deleted successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Delete success example",
                                    summary = "Currency exchange deleted successfully",
                                    value = """
                                            {
                                              "delete_currency_exchange": "true"
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
                    description = "Currency exchange not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Not found example",
                                    value = """
                                            {
                                              "error": "Currency exchange not found"
                                            }
                                            """
                            )
                    )
            )
    })
    @PreAuthorize("hasAnyAuthority('pricing:write')")
    @DeleteMapping
    public ResponseEntity<?> delete(
            @Parameter(
                    description = "UUID of the currency exchange to delete",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174001"
            )
            @RequestParam UUID id) {
        return currencyExchangeService.deleteById(id);
    }
}