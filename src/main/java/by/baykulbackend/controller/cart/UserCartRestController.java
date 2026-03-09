package by.baykulbackend.controller.cart;

import by.baykulbackend.database.dao.cart.Cart;
import by.baykulbackend.database.dao.cart.CartProduct;
import by.baykulbackend.database.dto.security.Views;
import by.baykulbackend.database.repository.cart.ICartRepository;
import by.baykulbackend.exceptions.NotFoundException;
import by.baykulbackend.services.cart.CartService;
import by.baykulbackend.services.user.AuthService;
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
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cart/user")
@RequiredArgsConstructor
@Tag(name = "User's cart", description = "User's cart management")
public class UserCartRestController {
    private final ICartRepository iCartRepository;
    private final CartService cartService;
    private final AuthService authService;

    @Operation(
            summary = "Get user's cart",
            description = "Retrieves the cart of the currently authenticated user. Requires my-cart:read permission.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User's cart retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Views.CartFullView.class),
                            examples = @ExampleObject(
                                    name = "User cart response example",
                                    summary = "User's cart details",
                                    value = """
                                            {
                                              "id": "123e4567-e89b-12d3-a456-426614174001",
                                              "createdTs": "2024-01-15T10:30:00",
                                              "updatedTs": "2024-01-20T14:45:30",
                                              "user": {
                                                "id": "123e4567-e89b-12d3-a456-426614174000",
                                                "login": "john_doe"
                                              },
                                              "cartProducts": [
                                                {
                                                  "id": "30e9276f-ccce-45a7-9c28-e1ce22254eea",
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
                    description = "Cart not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Not found example",
                                    value = """
                                            {
                                              "error": "Cart not found"
                                            }
                                            """
                            )
                    )
            )
    })
    @GetMapping
    @PreAuthorize("hasAnyAuthority('my-cart:read')")
    @JsonView(Views.CartFullView.class)
    public Cart get() {
        return iCartRepository.findByUserLogin(authService.getAuthInfo().getPrincipal().toString())
                .orElseThrow(() -> new NotFoundException("Cart not found"));
    }

    @Operation(
            summary = "Add part to user's cart",
            description = "Adds a part to the currently authenticated user's cart. Requires my-cart:write permission.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Part added to cart successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Add part success example",
                                    summary = "Part added successfully",
                                    value = """
                                            {
                                              "add_cart": "true",
                                              "id": "30e9276f-ccce-45a7-9c28-e1ce22254eea"
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
                    description = "Cart or part not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Not found example",
                                    value = """
                                            {
                                              "error": "User's cart not found"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflict - part storage count is zero",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Conflict example",
                                    summary = "Part not available",
                                    value = """
                                            {
                                              "add_cart": "false",
                                              "storage_empty": "true",
                                              "error": "Part storage count is zero"
                                            }
                                            """
                            )
                    )
            )
    })
    @PostMapping("/add")
    @PreAuthorize("hasAnyAuthority('my-cart:write')")
    public ResponseEntity<?> addPart(
            @Parameter(
                    description = "UUID of the part to add to cart",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000"
            )
            @RequestParam UUID partId) {
        return cartService.addPartToCart(partId);
    }

    @Operation(
            summary = "Update cart product in user's cart",
            description = "Updates a cart product in the currently authenticated user's cart. " +
                    "Requires my-cart:write permission.",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Cart product data to update",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = CartProduct.class),
                            examples = @ExampleObject(
                                    name = "Update cart product request example",
                                    summary = "Update cart product quantity",
                                    value = """
                                            {
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
                    description = "Cart product updated successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Update cart product success example",
                                    summary = "Cart product updated successfully",
                                    value = """
                                            {
                                              "update_cart": "true"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request - invalid cart product data",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Validation error example",
                                    summary = "Validation errors",
                                    value = """
                                            {
                                              "error": "Parts count must be greater than zero",
                                              "update_cart": "false"
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
                    description = "Cart or cart product not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Not found example",
                                    value = """
                                            {
                                              "error": "Cart product does not exist in user's cart"
                                            }
                                            """
                            )
                    )
            )
    })
    @PatchMapping("/update")
    @PreAuthorize("hasAnyAuthority('my-cart:write')")
    public ResponseEntity<?> updateCartProduct(
            @Parameter(
                    description = "UUID of the cart product to update",
                    required = true,
                    example = "30e9276f-ccce-45a7-9c28-e1ce22254eea"
            )
            @RequestParam UUID id,
            @RequestBody CartProduct cartProduct) {
        return cartService.updateUsersCartProductById(id, cartProduct);
    }

    @Operation(
            summary = "Clear user's cart",
            description = "Removes all cart products from the currently authenticated user's cart. " +
                    "Requires my-cart:write permission.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Cart cleared successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Clear cart success example",
                                    summary = "Cart cleared successfully",
                                    value = """
                                            {
                                              "clear_cart": "true"
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
                    description = "Cart not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Not found example",
                                    value = """
                                            {
                                              "error": "User's cart not found"
                                            }
                                            """
                            )
                    )
            )
    })
    @PostMapping("/clear")
    @PreAuthorize("hasAnyAuthority('my-cart:write')")
    public ResponseEntity<?> clearCart() {
        return cartService.clearUsersCart();
    }

    @Operation(
            summary = "Delete cart product from user's cart",
            description = "Removes a specific cart product from the currently authenticated user's cart. " +
                    "Requires my-cart:write permission.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Cart product deleted successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Delete cart product success example",
                                    summary = "Cart product deleted successfully",
                                    value = """
                                            {
                                              "delete_cart_product": "true"
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
                    description = "Cart or cart product not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Not found example",
                                    value = """
                                            {
                                              "error": "Cart product does not exist in user's cart",
                                              "delete_cart_product": "false"
                                            }
                                            """
                            )
                    )
            )
    })
    @DeleteMapping("/product")
    @PreAuthorize("hasAnyAuthority('my-cart:write')")
    public ResponseEntity<?> deleteCartProduct(
            @Parameter(
                    description = "UUID of the cart product to delete",
                    required = true,
                    example = "30e9276f-ccce-45a7-9c28-e1ce22254eea"
            )
            @RequestParam UUID id) {
        return cartService.deleteUsersCartProductById(id);
    }
}