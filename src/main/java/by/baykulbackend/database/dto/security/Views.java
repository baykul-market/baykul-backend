package by.baykulbackend.database.dto.security;

public class Views {

    // User views
    public interface UserView {
        interface Get {}
        interface Post {}
        interface Patch {}
    }

    public interface UserFullView extends UserView.Get, RefreshTokenView.Get, BalanceView.Get, CartView.Get {}

    public interface UserAdminView extends UserFullView {}

    // Refresh token views
    public interface RefreshTokenView {
        interface Get {}
        interface Post {}
        interface Patch {}
    }

    public interface RefreshTokenFullView extends RefreshTokenView.Get, UserView.Get {}

    // Balance views
    public interface BalanceView {
        interface Get {}
        interface Post {}
        interface Patch {}
    }

    public interface BalanceFullView extends BalanceView.Get, UserView.Get, BalanceHistoryView.Get {}

    // Balance history views
    public interface BalanceHistoryView {
        interface Get {}
        interface Post {}
        interface Patch {}
    }

    public interface BalanceHistoryFullView extends BalanceHistoryView.Get, BalanceView.Get, UserView.Get {}

    // Part views
    public interface PartView {
        interface Get {}
        interface Post {}
        interface Patch {}
    }

    // Cart views
    public interface CartView {
        interface Get {}
        interface Post {}
        interface Patch {}
    }

    public interface CartFullView extends CartView.Get, UserView.Get, CartProductView.Get, PartView.Get {}

    // Cart product views
    public interface CartProductView {
        interface Get {}
        interface Post {}
        interface Patch {}
    }

    public interface CartProductFullView extends CartProductView.Get, CartView.Get, PartView.Get {}

    // Order views
    public interface OrderView {
        interface Get {}
        interface Post {}
        interface Patch {}
    }

    public interface OrderFullView extends OrderView.Get, UserView.Get, OrderProductView.Get, PartView.Get {}

    // Order product views
    public interface OrderProductView {
        interface Get {}
        interface Post {}
        interface Patch {}
    }

    public interface OrderProductFullView extends OrderProductView.Get, OrderView.Get, PartView.Get, BillView.Get {}

    // Bill views
    public interface BillView {
        interface Get {}
        interface Post {}
        interface Patch {}
    }

    public interface BillFullView extends BillView.Get, OrderProductView.Get {}

    public interface BillCreateFullView extends BillView.Post, OrderProductView.Get {}

    // Currency exchange views
    public interface CurrencyExchangeView {
        interface Get {}
        interface Post {}
        interface Patch {}
    }

    // Price config views
    public interface PriceConfigView {
        interface Get {}
        interface Post {}
        interface Patch {}
    }
}

