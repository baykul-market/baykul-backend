package by.baykulbackend.database.repository.product;

import by.baykulbackend.database.dao.product.PartSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IPartSourceRepository extends JpaRepository<PartSource, UUID> {
}
