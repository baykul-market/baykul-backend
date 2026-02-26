package by.baykulbackend.database.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Schema(description = """
                      User role in the system. Defines permissions and access levels.
                      
                      **Available roles:**
                      
                      - **USER** - Standard user with permissions: users:read, balances:read, products:read, carts:read, orders:read
                      - **MANAGER** - Manager with permissions: users:read, balances:read/write, products:read/write, carts:read, orders:read/write, bills:read/write
                      - **ADMIN** - Full system access
                      """,
        enumAsRef = true
)
public enum Role {
    USER(Set.of(
            Permission.PROFILE_READ, Permission.PROFILE_WRITE,
            Permission.PRODUCT_READ,
            Permission.MY_BALANCE_READ,
            Permission.MY_CART_READ, Permission.MY_CART_WRITE,
            Permission.MY_ORDER_READ, Permission.MY_ORDER_WRITE
    )),
    MANAGER(Set.of(
            Permission.PROFILE_READ, Permission.PROFILE_WRITE, Permission.USERS_READ,
            Permission.PRODUCT_READ, Permission.PRODUCT_WRITE,
            Permission.MY_BALANCE_READ, Permission.ALL_BALANCE_READ, Permission.ALL_BALANCE_WRITE,
            Permission.MY_CART_READ, Permission.MY_CART_WRITE, Permission.ALL_CART_READ,
            Permission.MY_ORDER_READ, Permission.MY_ORDER_WRITE, Permission.ALL_ORDER_READ, Permission.ALL_ORDER_WRITE,
            Permission.ALL_BILL_READ, Permission.ALL_BILL_WRITE
    )),
    ADMIN(Set.of(
            Permission.PROFILE_READ, Permission.PROFILE_WRITE, Permission.USERS_READ, Permission.USERS_WRITE,
            Permission.PRODUCT_READ, Permission.PRODUCT_WRITE,
            Permission.MY_BALANCE_READ, Permission.ALL_BALANCE_READ, Permission.ALL_BALANCE_WRITE,
            Permission.MY_CART_READ, Permission.MY_CART_WRITE, Permission.ALL_CART_READ, Permission.ALL_CART_WRITE,
            Permission.MY_ORDER_READ, Permission.MY_ORDER_WRITE, Permission.ALL_ORDER_READ, Permission.ALL_ORDER_WRITE,
            Permission.ALL_BILL_READ, Permission.ALL_BILL_WRITE
    ));

    private final Set<Permission> permissions;

    private static final String DEFAULT_PREFIX = "ROLE_";

    Role(Set<Permission> permissions) {
        this.permissions = permissions;
    }

    public Set<SimpleGrantedAuthority> getAuthorities() {
        Set<SimpleGrantedAuthority> authorities = getPermissions().stream()
                .map(permission -> new SimpleGrantedAuthority(permission.getPermission()))
                .collect(Collectors.toSet());

        authorities.add(new SimpleGrantedAuthority(DEFAULT_PREFIX + this.name()));

        return authorities;
    }
}
