package by.baykulbackend.database.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "Permission enum defining system permissions")
public enum Permission {
    PROFILE_READ("profile:read"),                           // Read own profile and settings
    PROFILE_WRITE("profile:write"),                         // Edit own profile and settings
    USERS_READ("users:read"),                               // Read list of all registered users
    USERS_WRITE("users:write"),                             // Create, Block, Delete any user
    PRODUCT_READ("products:read"),                          // View products (Public/Shared)
    PRODUCT_WRITE("products:write"),                        // Manage products, Upload CSV
    MY_BALANCE_READ("my-balance:read"),                     // View own balance
    ALL_BALANCE_READ("all-balances:read"),                  // View any user's balance
    ALL_BALANCE_WRITE("all-balances:write"),                // Adjust balances manually
    MY_CART_READ("my-cart:read"),                           // View own cart
    MY_CART_WRITE("my-cart:write"),                         // Add/Remove items in own cart
    ALL_CART_READ("all-carts:read"),                        // View any user's cart (Support)
    ALL_CART_WRITE("all-carts:write"),                      // Add/Remove items in any cart
    MY_ORDER_READ("my-orders:read"),                        // View own order history
    MY_ORDER_WRITE("my-orders:write"),                      // Place a new order
    ALL_ORDER_READ("all-orders:read"),                      // View all system orders
    ALL_ORDER_WRITE("all-orders:write"),                    // Process/Cancel any order
    ALL_BILL_READ("all-bills:read"),                        // View all system bills
    ALL_BILL_WRITE("all-bills:write"),                      // Manage bills
    PRICING_READ("pricing:read"),                           // View all currency exchange rates
    PRICING_WRITE("pricing:write");                         // Manage currency exchange rates

    private final String permission;

    Permission(String permission) {
        this.permission = permission;
    }

}
