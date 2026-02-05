package by.baykulbackend.controller.balance;

import by.baykulbackend.database.dao.balance.Balance;
import by.baykulbackend.database.dto.balance.BalanceOperationDto;
import by.baykulbackend.database.dto.security.Views;
import by.baykulbackend.database.repository.balance.IBalanceRepository;
import by.baykulbackend.exceptions.NotFoundException;
import by.baykulbackend.services.balance.BalanceService;
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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/balance")
@RequiredArgsConstructor
@Tag(name = "Balance", description = "User's balance management")
public class BalanceRestController {
    private final IBalanceRepository iBalanceRepository;
    private final BalanceService balanceService;

    @Operation(
            summary = "Get all balances",
            description = "Retrieves all balances from the system with their users. Requires balances:write permission.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "List of balances retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = Views.BalanceView.Get.class)),
                            examples = @ExampleObject(
                                    name = "All balances response example",
                                    summary = "List of all balances",
                                    value = """
                                            [
                                              {
                                                "id": "123e4567-e89b-12d3-a456-426614174001",
                                                "createdTs": "2024-01-15T10:30:00",
                                                "updatedTs": "2024-01-20T14:45:30",
                                                "account": 2024.30
                                              },
                                              {
                                                "id": "522t4767-e89b-12d3-a456-426614174563",
                                                "createdTs": "2024-01-15T10:30:00",
                                                "updatedTs": "2024-01-20T14:45:30",
                                                "account": -123.00
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
    @PreAuthorize("hasAnyAuthority('balances:write')")
    @JsonView(Views.BalanceView.Get.class)
    public List<Balance> getAll() {
        return iBalanceRepository.findAll();
    }

    @Operation(
            summary = "Get balance by ID",
            description = "Retrieves a specific balance by UUID with their user. Requires balances:write permission.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Balance retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Views.BalanceFullView.class),
                            examples = @ExampleObject(
                                    name = "Single balance response example",
                                    summary = "Balance details",
                                    value = """
                                            {
                                              "id": "123e4567-e89b-12d3-a456-426614174001",
                                              "createdTs": "2024-01-15T10:30:00",
                                              "updatedTs": "2024-01-20T14:45:30",
                                              "account": 2024.30,
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
                                              "balanceHistoryList": [
                                                {
                                                  "id": "30e9276f-ccce-45a7-9c28-e1ce22254eea"
                                                  "amount": 10.00,
                                                  "operationType": "REPLENISHMENT",
                                                  "resultAccount": 20.00
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
                    description = "Balance not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Not found example",
                                    value = """
                                            {
                                              "error": "Balance not found",
                                            }
                                            """
                            )
                    )
            )
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('balances:write')")
    @JsonView(Views.BalanceFullView.class)
    public Balance getOne(
            @Parameter(
                    description = "UUID of the balance to retrieve",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000"
            )
            @PathVariable UUID id) {
        return iBalanceRepository.findById(id).orElseThrow(() -> new NotFoundException("Balance not found"));
    }

    @Operation(
            summary = "Get balance by user ID",
            description = "Retrieves a specific balance by user UUID with their user. Requires balances:write permission.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Balance retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Views.BalanceFullView.class),
                            examples = @ExampleObject(
                                    name = "Single balance response example",
                                    summary = "Balance details",
                                    value = """
                                            {
                                              "id": "123e4567-e89b-12d3-a456-426614174001",
                                              "createdTs": "2024-01-15T10:30:00",
                                              "updatedTs": "2024-01-20T14:45:30",
                                              "account": 2024.30,
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
                                              "balanceHistoryList": [
                                                {
                                                  "id": "30e9276f-ccce-45a7-9c28-e1ce22254eea"
                                                  "amount": 10.00,
                                                  "operationType": "REPLENISHMENT",
                                                  "resultAccount": 20.00
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
                    description = "Balance not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Not found example",
                                    value = """
                                            {
                                              "error": "User balance not found",
                                            }
                                            """
                            )
                    )
            )
    })
    @GetMapping("/user/{id}")
    @PreAuthorize("hasAnyAuthority('balances:write')")
    @JsonView(Views.BalanceFullView.class)
    public Balance getByUserId(
            @Parameter(
                    description = "UUID of the user",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000"
            )
            @PathVariable UUID id) {
        return iBalanceRepository.findByUserId(id).orElseThrow(() -> new NotFoundException("User balance not found"));
    }

    @Operation(
            summary = "Perform balance operation",
            description = "Performs a balance replenishment, withdrawal, or payment operation. " +
                    "Requires 'balances:write' authority.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Balance operation request data",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = BalanceOperationDto.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Balance replenishment",
                                            value = """
                                            {
                                                "balanceId": "123e4567-e89b-12d3-a456-426614174001",
                                                "amount": 1000.50,
                                                "operationType": "REPLENISHMENT",
                                                "description": "Replenishment via bank card"
                                            }
                                            """
                                    ),
                                    @ExampleObject(
                                            name = "Funds withdrawal",
                                            value = """
                                            {
                                                "balanceId": "123e4567-e89b-12d3-a456-426614174002",
                                                "amount": 500.00,
                                                "operationType": "WITHDRAWAL",
                                                "description": "Withdrawal to card"
                                            }
                                            """
                                    ),
                                    @ExampleObject(
                                            name = "Service payment",
                                            value = """
                                            {
                                                "userId": "123e4567-e89b-12d3-a456-426614174003",
                                                "amount": 250.75,
                                                "operationType": "PAYMENT",
                                                "description": "Internet service payment"
                                            }
                                            """
                                    )
                            }
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "Balance operation successful - no content returned"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request - invalid data format",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Bad request example",
                                    summary = "Invalid token format",
                                    value = """
                                            {
                                              "error": "Operation amount must be greater than zero"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Not found - balance doesn't exist in database",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Not found example",
                                    summary = "Balance not found",
                                    value = """
                                            {
                                              "error": "User balance not found"
                                            }
                                            """
                            )
                    )
            )
    })
    @Transactional
    @PostMapping("/operation")
    @PreAuthorize("hasAnyAuthority('balances:write')")
    public void operation(@RequestBody BalanceOperationDto balanceOperationDto) {
        balanceService.processBalance(balanceOperationDto);
    }
}
