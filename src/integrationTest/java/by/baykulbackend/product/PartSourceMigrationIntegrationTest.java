package by.baykulbackend.product;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.sql.DriverManager;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class PartSourceMigrationIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> DATABASE = new PostgreSQLContainer<>("postgres:17-alpine");

    @Test
    void upgradesThePreviousReleaseWithoutChangingIdsOrdersOrCartReferences() throws Exception {
        migrate("db/previous-version.yaml");
        JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword()));
        UUID user = UUID.randomUUID();
        UUID part = UUID.randomUUID();
        UUID cart = UUID.randomUUID();
        UUID order = UUID.randomUUID();
        UUID orderPart = UUID.randomUUID();
        jdbc.update("INSERT INTO users (id, login, password, email, role, blocked, can_pay_later, localization) VALUES (?, 'migration', 'test', 'migration@example.test', 'USER', false, true, 'RUS')", user);
        jdbc.update("INSERT INTO parts (id, article, brand, name, price, currency, min_count, storage_count, weight) VALUES (?, 'A-01', 'BMW', 'Original', 7.50, 'EUR', 1, 4, 2.5)", part);
        jdbc.update("INSERT INTO carts (id, user_id) VALUES (?, ?)", cart, user);
        jdbc.update("INSERT INTO cart_products (id, cart_id, part_id, parts_count) VALUES (?, ?, ?, 2)", UUID.randomUUID(), cart, part);
        jdbc.update("INSERT INTO orders (id, number, user_id, status, paid) VALUES (?, 100000, ?, 'COMPLETED', true)", order, user);
        jdbc.update("INSERT INTO order_products (id, order_id, part_id, parts_count, price, currency, status, paid) VALUES (?, ?, ?, 2, 12.34, 'USD', 'DELIVERED', true)", orderPart, order, part);
        migrate("db/changelog/db.changelog-master.yaml");
        migrate("db/changelog/db.changelog-master.yaml");
        assertEquals(part, jdbc.queryForObject("SELECT part_id FROM cart_products WHERE cart_id = ?", UUID.class, cart));
        assertEquals(part, jdbc.queryForObject("SELECT part_id FROM order_products WHERE id = ?", UUID.class, orderPart));
        assertEquals(new BigDecimal("12.34"), jdbc.queryForObject("SELECT price FROM order_products WHERE id = ?", BigDecimal.class, orderPart));
        assertEquals("USD", jdbc.queryForObject("SELECT currency FROM order_products WHERE id = ?", String.class, orderPart));
        assertEquals(new BigDecimal("7.50"), jdbc.queryForObject("SELECT price FROM parts WHERE id = ?", BigDecimal.class, part));
        assertEquals("Original", jdbc.queryForObject("SELECT name FROM parts WHERE id = ?", String.class, part));
        assertEquals("ACTIVE", jdbc.queryForObject("SELECT s.status FROM parts p JOIN part_sources s ON s.id = p.source_id WHERE p.id = ?", String.class, part));
        jdbc.update("INSERT INTO parts (id, article, brand, price, currency, min_count, source_id) SELECT ?, article, 'BOSCH', price, currency, min_count, source_id FROM parts WHERE id = ?", UUID.randomUUID(), part);
        assertEquals(2L, jdbc.queryForObject("SELECT count(*) FROM parts WHERE article = 'A-01'", Long.class));
    }

    private void migrate(String changelog) throws Exception {
        try (var connection = DriverManager.getConnection(DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword());
                var liquibase = new Liquibase(changelog, new liquibase.integration.spring.SpringResourceAccessor(new org.springframework.core.io.DefaultResourceLoader()),
                        DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection)))) {
            liquibase.update(new Contexts(), new LabelExpression());
        }
    }
}
