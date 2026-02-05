package by.baykulbackend.database.dto.security;

public class Views {

    // User views
    public interface UserView {
        interface Get {}
        interface Post {}
        interface Put {}
    }

    public interface UserFullView extends UserView.Get, RefreshTokenView.Get, BalanceView.Get, CartView.Get {}

    // Refresh token views
    public interface RefreshTokenView {
        interface Get {}
        interface Post {}
        interface Put {}
    }

    public interface RefreshTokenFullView extends RefreshTokenView.Get, UserView.Get {}

    // Balance views
    public interface BalanceView {
        interface Get {}
        interface Post {}
        interface Put {}
    }

    public interface BalanceFullView extends BalanceView.Get, UserView.Get, BalanceHistoryView.Get {}

    // Balance history views
    public interface BalanceHistoryView {
        interface Get {}
        interface Post {}
        interface Put {}
    }

    public interface BalanceHistoryFullView extends BalanceHistoryView.Get, BalanceView.Get, UserView.Get {}

    // Part views
    public interface PartView {
        interface Get {}
        interface Post {}
        interface Put {}
    }

    // Cart views
    public interface CartView {
        interface Get {}
        interface Post {}
        interface Put {}
    }

    public interface CartFullView extends CartView.Get, UserView.Get, CartProductView.Get, PartView.Get {}

    // Cart product views
    public interface CartProductView {
        interface Get {}
        interface Post {}
        interface Put {}
    }

    public interface CartProductFullView extends CartProductView.Get, CartView.Get, PartView.Get {}
}

