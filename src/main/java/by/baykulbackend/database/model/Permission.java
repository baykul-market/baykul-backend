package by.baykulbackend.database.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "Permission enum defining system permissions")
public enum Permission {
    USERS_READ("users:read"),
    USERS_WRITE("users:write"),
    PRODUCT_READ("products:read"),
    PRODUCT_WRITE("products:write"),
    BALANCE_READ("balances:read"),
    BALANCE_WRITE("balances:write"),
    CART_READ("carts:read"),
    CART_WRITE("carts:write");

    private final String permission;

    Permission(String permission) {
        this.permission = permission;
    }

}
