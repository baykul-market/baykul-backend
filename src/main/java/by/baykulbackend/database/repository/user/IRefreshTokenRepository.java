package by.baykulbackend.database.repository.user;

import by.baykulbackend.database.dao.user.RefreshToken;
import by.baykulbackend.database.dao.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IRefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    List<RefreshToken> findRefreshTokenByUser(User user);
    RefreshToken findRefreshTokenByName(String name);
    RefreshToken findRefreshTokenByUserAgentAndIpAddress(String userAgent, String ipAddress);
    void deleteByUser(User user);
}
