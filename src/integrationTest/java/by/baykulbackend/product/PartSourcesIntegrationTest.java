package by.baykulbackend.product;

import by.baykulbackend.database.dao.finance.Currency;
import by.baykulbackend.database.dao.product.Part;
import by.baykulbackend.database.dao.product.PartSource;
import by.baykulbackend.database.dto.product.PartImportView;
import by.baykulbackend.database.model.Role;
import by.baykulbackend.exceptions.CatalogConflictException;
import by.baykulbackend.security.JwtAuthentication;
import by.baykulbackend.services.finance.PriceService;
import by.baykulbackend.services.product.PartCatalogGuard;
import by.baykulbackend.services.product.PartImportService;
import by.baykulbackend.services.product.PartImportStore;
import by.baykulbackend.services.product.PartSourceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class PartSourcesIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> DATABASE = new PostgreSQLContainer<>("postgres:17-alpine");
    static final String HEADER = "article;name;weight;min_count;storage_count;return_part;price;brand\n";
    static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    static final UUID CART_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", DATABASE::getJdbcUrl);
        registry.add("spring.datasource.username", DATABASE::getUsername);
        registry.add("spring.datasource.password", DATABASE::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.datasource.hikari.data-source-properties.reWriteBatchedInserts", () -> "true");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.liquibase.enabled", () -> "true");
        registry.add("app.imports.directory", () -> "build/integration-uploads");
        registry.add("spring.servlet.multipart.max-file-size", () -> "200MB");
        registry.add("spring.servlet.multipart.max-request-size", () -> "210MB");
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired jakarta.persistence.EntityManager entities;
    @Autowired PartImportService imports;
    @Autowired PartImportStore store;
    @Autowired PartSourceService sources;
    @Autowired PartCatalogGuard guard;
    @Autowired PlatformTransactionManager manager;
    @Autowired MockMvc mvc;
    @org.springframework.boot.test.web.server.LocalServerPort int port;
    @Autowired by.baykulbackend.security.JwtProvider tokens;
    @MockBean(name = "partImportExecutor") ThreadPoolTaskExecutor executor;
    @MockBean PriceService prices;

    @BeforeEach
    void setup() {
        jdbc.execute("TRUNCATE users, part_sources CASCADE");
        jdbc.update("INSERT INTO part_sources (id, name, status, system_source) VALUES (?, 'Legacy', 'ACTIVE', true), (?, 'Manual', 'ACTIVE', true)",
                PartSource.LEGACY_ID, PartSource.MANUAL_ID);
        jdbc.update("INSERT INTO users (id, login, password, email, role, blocked, can_pay_later, localization) VALUES (?, 'buyer', 'test', 'buyer@example.test', 'USER', false, true, 'RUS')", USER_ID);
        jdbc.update("INSERT INTO carts (id, user_id) VALUES (?, ?)", CART_ID, USER_ID);
        doAnswer(call -> { call.<Runnable>getArgument(0).run(); return null; }).when(executor).execute(any(Runnable.class));
        when(prices.getSystemCurrency()).thenReturn(Currency.EUR);
        when(prices.calculateProductPrice(any(Part.class), anyBoolean(), any())).thenAnswer(call -> call.<Part>getArgument(0).getPrice());
        when(prices.calculateProductPrice(any(Part.class), anyBoolean())).thenAnswer(call -> call.<Part>getArgument(0).getPrice());
    }

    @Test
    void independentManufacturersAndSourcesKeepTheirIdsDuringReplacementAndReappearance() {
        UUID source = sources.create("Supplier A").id();
        UUID other = sources.create("Supplier B").id();
        assertEquals(PartSource.Status.HIDDEN, sources.get(source).status());
        apply(source, "A;First;1;1;5;0;10;BMW\nA;Second;2;1;8;0;20;BOSCH\nOLD;Old;;1;;0;4;BMW\n");
        apply(other, "A;Other;1;1;3;0;99;BMW\n");
        UUID original = partId(source, "A", "BMW");
        UUID removed = partId(source, "OLD", "BMW");
        var preview = upload(source, "A;Changed;;1;;0;12;BMW\nNEW;New;;1;;0;2;BMW\n");
        assertEquals(1, preview.added());
        assertEquals(1, preview.updated());
        assertEquals(2, preview.removed());
        assertEquals(new BigDecimal("10.00"), price(original));
        imports.apply(source, preview.id(), false);
        assertEquals(original, partId(source, "A", "BMW"));
        assertEquals(new BigDecimal("12.00"), price(original));
        assertFalse(present(removed));
        assertEquals(new BigDecimal("99.00"), price(partId(other, "A", "BMW")));
        assertEquals(PartSource.Status.HIDDEN, sources.get(source).status());
        apply(source, "OLD;Back;;1;;0;6;BMW\n");
        assertEquals(removed, partId(source, "OLD", "BMW"));
        assertTrue(present(removed));
    }

    @Test
    void lastValidDuplicateWinsAcrossChunksAndPartialReplacementRequiresConfirmation() {
        UUID source = sources.create("Duplicates").id();
        apply(source, "OLD;Old;;1;;0;1;BMW\n");
        StringBuilder data = new StringBuilder("A;First;;1;;0;1;BMW\nA;Early repeat;;1;;0;2;BMW\n");
        for (int i = 0; i < 5000; i++) {
            data.append("B").append(i).append(";Other;;1;;0;1;BMW\n");
        }
        data.append("A;Last;;1;;0;7;BMW\nA;Invalid;;1;;0;-3;BMW\n");
        PartImportView job = upload(source, data.toString());
        assertEquals(2, job.duplicates());
        assertEquals(1, job.skipped());
        assertEquals(5005, store.errors(job.id(), org.springframework.data.domain.PageRequest.of(0, 50)).getContent().getFirst().getRowNumber());
        var conflict = assertThrows(CatalogConflictException.class, () -> imports.apply(source, job.id(), false));
        assertEquals("SKIPPED_ROWS_CONFIRMATION", conflict.getCode());
        assertTrue(present(partId(source, "OLD", "BMW")));
        imports.apply(source, job.id(), true);
        assertEquals(new BigDecimal("7.00"), price(partId(source, "A", "BMW")));
        assertFalse(present(partId(source, "OLD", "BMW")));
        assertEquals("COMPLETED", imports.apply(source, job.id(), true).status());
    }

    @Test
    void staleConfirmationRequiresRecheckAndOnlyOnePendingImportIsAllowed() {
        UUID source = sources.create("Versioned").id();
        PartImportView job = upload(source, "A;Name;;1;;0;1;BMW\n");
        assertEquals("IMPORT_PENDING", assertThrows(CatalogConflictException.class,
                () -> upload(source, "B;Name;;1;;0;1;BMW\n")).getCode());
        sources.update(source, "Renamed", PartSource.Status.ACTIVE);
        assertEquals("SOURCE_CHANGED", assertThrows(CatalogConflictException.class,
                () -> imports.apply(source, job.id(), false)).getCode());
        imports.recheck(source, job.id());
        assertEquals("COMPLETED", imports.apply(source, job.id(), false).status());
    }

    @Test
    void failedPublicationRollsBackBothUpdatesAndRemoval() {
        UUID source = sources.create("Atomic").id();
        apply(source, "A;Old;;1;;0;1;BMW\nOLD;Old;;1;;0;1;BMW\n");
        UUID original = partId(source, "A", "BMW");
        PartImportView job = upload(source, "A;Changed;;1;;0;20;BMW\nFAIL;New;;1;;0;2;BMW\n");
        jdbc.execute("ALTER TABLE parts ADD CONSTRAINT integration_publication_failure CHECK (article <> 'FAIL')");
        try {
            assertEquals("FAILED", imports.apply(source, job.id(), false).status());
            assertEquals(new BigDecimal("1.00"), price(original));
            assertTrue(present(partId(source, "OLD", "BMW")));
            assertEquals(2L, jdbc.queryForObject("SELECT count(*) FROM parts WHERE source_id = ?", Long.class, source));
        } finally {
            jdbc.execute("ALTER TABLE parts DROP CONSTRAINT integration_publication_failure");
        }
    }

    @Test
    void emptyAllInvalidMalformedAndInterruptedImportsNeverChangeCatalog() {
        UUID source = sources.create("Failures").id();
        apply(source, "A;Kept;;1;;0;1;BMW\n");
        assertEquals("FAILED", upload(source, "").status());
        assertEquals("FAILED", upload(source, "bad\n").status());
        byte[] invalidUtf8 = (HEADER + "A;Changed;;1;;0;7;BMW\n").getBytes(StandardCharsets.UTF_8);
        byte[] corrupt = java.util.Arrays.copyOf(invalidUtf8, invalidUtf8.length + 1);
        corrupt[corrupt.length - 1] = (byte) 0xFF;
        assertEquals("FAILED", imports.upload(source, new MockMultipartFile("csvFile", "bad.csv", "text/csv", corrupt), "manager").status());
        List<Runnable> queue = new ArrayList<>();
        doAnswer(call -> { queue.add(call.getArgument(0)); return null; }).when(executor).execute(any(Runnable.class));
        var interrupted = upload(source, "A;Changed;;1;;0;8;BMW\n");
        assertEquals("QUEUED", interrupted.status());
        imports.recoverInterrupted();
        assertEquals("FAILED", store.get(interrupted.id()).status());
        queue.getFirst().run();
        assertEquals(new BigDecimal("1.00"), price(partId(source, "A", "BMW")));
    }

    @Test
    void preparedImportsSurviveRestartAndArchivingCancelsTheirStaging() {
        UUID source = sources.create("Retained preview").id();
        var job = upload(source, "A;Prepared;;1;;0;1;BMW\n");
        imports.recoverInterrupted();
        assertEquals("READY", store.get(job.id()).status());
        sources.archive(source);
        assertEquals("CANCELLED", store.get(job.id()).status());
        assertEquals(0L, jdbc.queryForObject("SELECT count(*) FROM part_import_rows", Long.class));
    }

    @Test
    void delayedStockUpdateDoesNotRestoreRemovedOfferOrOverwriteImportedPrice() throws Exception {
        UUID source = sources.create("Concurrent inventory").id();
        apply(source, "A;Original;1;1;10;0;1;BMW\n");
        UUID id = partId(source, "A", "BMW");
        new TransactionTemplate(manager).executeWithoutResult(tx -> {
            Part stale = entities.find(Part.class, id);
            CompletableFuture.runAsync(() -> {
                apply(source, "A;Imported;2;1;20;0;9;BMW\n");
                apply(source, "B;Replacement;;1;;0;2;BMW\n");
            }).join();
            stale.setStorageCount(9);
            entities.flush();
        });
        assertFalse(present(id));
        assertEquals(new BigDecimal("9.00"), price(id));
        assertEquals("Imported", jdbc.queryForObject("SELECT name FROM parts WHERE id = ?", String.class, id));
    }

    @Test
    void cancellationAndRetentionDiscardStagingButKeepHistory() {
        UUID source = sources.create("Retention").id();
        var job = upload(source, "A;Valid;;1;;0;1;BMW\nbad\n");
        imports.cancel(source, job.id());
        assertEquals("CANCELLED", imports.cancel(source, job.id()).status());
        assertEquals(0L, jdbc.queryForObject("SELECT count(*) FROM part_import_rows", Long.class));
        var expiring = upload(source, "A;Valid;;1;;0;1;BMW\n");
        jdbc.update("UPDATE part_imports SET created_ts = CURRENT_TIMESTAMP - INTERVAL '25 hours' WHERE id = ?", expiring.id());
        jdbc.update("UPDATE part_imports SET updated_ts = CURRENT_TIMESTAMP - INTERVAL '31 days' WHERE id = ?", job.id());
        imports.cleanup();
        assertEquals("CANCELLED", store.get(expiring.id()).status());
        assertEquals(0L, jdbc.queryForObject("SELECT count(*) FROM part_import_errors", Long.class));
        assertEquals(2L, jdbc.queryForObject("SELECT count(*) FROM part_imports", Long.class));
    }

    @Test
    void legacyUploadKeepsItsResponseAndDoesNotRemoveMissingParts() {
        var first = imports.uploadLegacy(file("A;First;2;1;7;0;1;BMW\nOLD;Old;;1;;0;2;BMW\n"), "manager");
        assertEquals(200, first.getStatusCode().value());
        assertEquals(2, first.getBody().getSaved());
        UUID original = partId(PartSource.LEGACY_ID, "A", "BMW");
        var second = imports.uploadLegacy(file("A;Updated;;1;;0;3;BMW\nA;Other;;1;;0;5;BOSCH\nbad\n"), "manager");
        assertEquals(207, second.getStatusCode().value());
        assertEquals(1, second.getBody().getSaved());
        assertEquals(1, second.getBody().getUpdated());
        assertEquals(1, second.getBody().getSkipped());
        assertEquals(original, partId(PartSource.LEGACY_ID, "A", "BMW"));
        assertEquals(2.0, jdbc.queryForObject("SELECT weight FROM parts WHERE id = ?", Double.class, original));
        assertEquals(7, jdbc.queryForObject("SELECT storage_count FROM parts WHERE id = ?", Integer.class, original));
        assertTrue(present(partId(PartSource.LEGACY_ID, "OLD", "BMW")));
        sources.archive(PartSource.LEGACY_ID);
        assertThrows(CatalogConflictException.class, () -> imports.uploadLegacy(file("A;Name;;1;;0;1;BMW\n"), "manager"));
    }

    @Test
    void apiPermissionsAndHiddenCatalogAreEnforcedAcrossSearchAndCheckout() throws Exception {
        UUID source = sources.create("Source").id();
        apply(source, "A;Name;;1;4;0;2;BMW\nA;Name;;1;4;0;3;BOSCH\n");
        UUID part = partId(source, "A", "BMW");
        sources.update(source, null, PartSource.Status.ACTIVE);
        mvc.perform(get("/api/v1/product/sources")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/product/sources").with(as(Role.USER))).andExpect(status().isForbidden());
        for (Role role : List.of(Role.MANAGER, Role.ADMIN)) {
            mvc.perform(get("/api/v1/product/sources").with(as(role))).andExpect(status().isOk());
        }
        mvc.perform(get("/api/v1/product/search/exact/article").param("article", "A").with(as(Role.USER)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("AMBIGUOUS_PART"));
        mvc.perform(post("/api/v1/product/search/articles").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"articles\":[\"A\"]}").with(as(Role.USER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.parts.length()").value(2));
        mvc.perform(post("/api/v1/cart/user/add").param("partId", part.toString()).with(as(Role.USER)))
                .andExpect(status().isOk());
        sources.update(source, null, PartSource.Status.HIDDEN);
        for (String path : List.of("/api/v1/product/search?text=A", "/api/v1/product/search/filter?article=A",
                "/api/v1/product/search/exact/name?name=Name", "/api/v1/product/search/exact/brand?brand=BMW")) {
            mvc.perform(get(path).with(as(Role.USER))).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        }
        mvc.perform(get("/api/v1/product").with(as(Role.USER))).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/v1/product/id").param("id", part.toString()).with(as(Role.USER))).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/cart/user").with(as(Role.USER))).andExpect(status().isOk())
                .andExpect(jsonPath("$.cartProducts[0].part.available").value(false));
        mvc.perform(post("/api/v1/cart/user/add").param("partId", part.toString()).with(as(Role.USER))).andExpect(status().isConflict());
        mvc.perform(post("/api/v1/order/user/create").with(as(Role.USER))).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PART_UNAVAILABLE"));
        assertEquals(1L, jdbc.queryForObject("SELECT count(*) FROM cart_products", Long.class));
        assertEquals(0L, jdbc.queryForObject("SELECT count(*) FROM orders", Long.class));
        sources.archive(source);
        assertEquals(2L, jdbc.queryForObject("SELECT count(*) FROM parts", Long.class));
        assertEquals(PartSource.Status.ARCHIVED, sources.archive(source).status());
    }

    @Test
    void sourceExclusiveChangesWaitForCheckoutSharedLock() throws Exception {
        UUID source = sources.create("Locked").id();
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        var reader = CompletableFuture.runAsync(() -> new TransactionTemplate(manager).executeWithoutResult(tx -> {
            guard.lockSource(source, false);
            locked.countDown();
            try {
                assertTrue(release.await(10, TimeUnit.SECONDS));
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        }));
        assertTrue(locked.await(10, TimeUnit.SECONDS));
        var writer = CompletableFuture.runAsync(() -> sources.archive(source));
        try {
            assertThrows(java.util.concurrent.TimeoutException.class, () -> writer.get(200, TimeUnit.MILLISECONDS));
        } finally {
            release.countDown();
        }
        reader.get(10, TimeUnit.SECONDS);
        writer.get(10, TimeUnit.SECONDS);
        assertEquals(PartSource.Status.ARCHIVED, sources.get(source).status());
    }

    @Test
    @org.junit.jupiter.api.condition.EnabledIfSystemProperty(named = "largeImportTest", matches = "true")
    void largeFileUsesRealHttpAndKeepsCurrentCatalogUntilPublication() throws Exception {
        UUID source = sources.create("Large HTTP import").id();
        apply(source, "OLD;Original;;1;;0;1;BMW\n");
        sources.update(source, null, PartSource.Status.ACTIVE);
        var path = java.nio.file.Path.of("build", "large-import.csv");
        long rows = 0;
        long bytes = HEADER.length();
        String name = "Item " + "x".repeat(200);
        try (var out = java.nio.file.Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            out.write(HEADER);
            while (true) {
                String line = "L" + rows + ";" + name + ";1;1;2;0;3;BMW\n";
                if (bytes + line.length() > PartImportService.MAX_BYTES - 1024) {
                    break;
                }
                out.write(line);
                bytes += line.length();
                rows++;
            }
        }
        var user = new by.baykulbackend.database.dao.user.User();
        user.setId(USER_ID); user.setLogin("buyer"); user.setRole(Role.MANAGER); user.setBlocked(false);
        String token = tokens.generateAccessToken(user);
        String boundary = "baykulIntegrationBoundary";
        var body = java.net.http.HttpRequest.BodyPublishers.concat(
                java.net.http.HttpRequest.BodyPublishers.ofString("--" + boundary
                        + "\r\nContent-Disposition: form-data; name=\"csvFile\"; filename=\"large.csv\"\r\nContent-Type: text/csv\r\n\r\n"),
                java.net.http.HttpRequest.BodyPublishers.ofFile(path),
                java.net.http.HttpRequest.BodyPublishers.ofString("\r\n--" + boundary + "--\r\n"));
        var request = java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://localhost:" + port + "/api/v1/product/sources/" + source + "/imports"))
                .header("Authorization", "Bearer " + token).header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .timeout(java.time.Duration.ofMinutes(10)).POST(body).build();
        var worker = new ThreadPoolTaskExecutor();
        worker.setCorePoolSize(1);
        worker.setMaxPoolSize(1);
        worker.setQueueCapacity(8);
        worker.initialize();
        doAnswer(call -> { worker.execute(call.<Runnable>getArgument(0)); return null; }).when(executor).execute(any(Runnable.class));
        long start = System.nanoTime();
        long deadline = start + TimeUnit.MINUTES.toNanos(10);
        long peak = 0;
        try (var http = java.net.http.HttpClient.newHttpClient()) {
            var response = http.sendAsync(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            while (!response.isDone()) {
                peak = Math.max(peak, java.lang.management.ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed());
                assertEquals(1L, jdbc.queryForObject("SELECT count(*) FROM parts WHERE source_id = ? AND catalog_present = true", Long.class, source));
                java.util.concurrent.locks.LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(100));
            }
            var result = response.get(10, TimeUnit.MINUTES);
            assertEquals(202, result.statusCode(), result.body());
            var parsed = new com.fasterxml.jackson.databind.ObjectMapper().readTree(result.body());
            UUID jobId = UUID.fromString(parsed.get("id").asText());
            while (List.of("QUEUED", "PROCESSING").contains(store.get(jobId).status())) {
                assertTrue(System.nanoTime() < deadline, "Import processing timed out");
                peak = Math.max(peak, java.lang.management.ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed());
                assertEquals(1L, jdbc.queryForObject("SELECT count(*) FROM parts WHERE source_id = ? AND catalog_present = true", Long.class, source));
                java.util.concurrent.locks.LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(100));
            }
            assertEquals("READY", store.get(jobId).status(), store.get(jobId).errorMessage());
            assertEquals(rows, store.get(jobId).added());
            imports.apply(source, jobId, false);
            while ("APPLYING".equals(store.get(jobId).status())) {
                assertTrue(System.nanoTime() < deadline, "Publication timed out");
                peak = Math.max(peak, java.lang.management.ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed());
                long count = jdbc.queryForObject("SELECT count(*) FROM parts WHERE source_id = ? AND catalog_present = true", Long.class, source);
                assertTrue(count == 1 || count == rows, "A partial catalog was visible");
                java.util.concurrent.locks.LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(100));
            }
            assertEquals("COMPLETED", store.get(jobId).status(), store.get(jobId).errorMessage());
            assertEquals(rows, jdbc.queryForObject("SELECT count(*) FROM parts WHERE source_id = ? AND catalog_present = true", Long.class, source));
            System.out.printf("LARGE_IMPORT_RESULT bytes=%d rows=%d elapsedSeconds=%.1f peakObservedHeapMiB=%d maxHeapMiB=%d%n",
                    bytes, rows, (System.nanoTime() - start) / 1_000_000_000.0, peak / 1024 / 1024,
                    Runtime.getRuntime().maxMemory() / 1024 / 1024);
        } finally {
            worker.shutdown();
            java.nio.file.Files.deleteIfExists(path);
        }
    }

    static RequestPostProcessor as(Role role) {
        var auth = new JwtAuthentication();
        auth.setAuthenticated(true);
        auth.setLogin("buyer");
        auth.setId(USER_ID.toString());
        auth.setRole(role);
        return authentication(auth);
    }

    static MockMultipartFile file(String rows) {
        return new MockMultipartFile("csvFile", "parts.csv", "text/csv", (HEADER + rows).getBytes(StandardCharsets.UTF_8));
    }

    PartImportView upload(UUID source, String rows) {
        return imports.upload(source, file(rows), "manager");
    }

    void apply(UUID source, String rows) {
        PartImportView job = upload(source, rows);
        assertEquals("READY", job.status(), job.errorMessage());
        assertEquals("COMPLETED", imports.apply(source, job.id(), false).status());
    }

    UUID partId(UUID source, String article, String brand) {
        return jdbc.queryForObject("SELECT id FROM parts WHERE source_id = ? AND article = ? AND brand = ?", UUID.class, source, article, brand);
    }

    BigDecimal price(UUID id) {
        return jdbc.queryForObject("SELECT price FROM parts WHERE id = ?", BigDecimal.class, id);
    }

    boolean present(UUID id) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT catalog_present FROM parts WHERE id = ?", Boolean.class, id));
    }
}
