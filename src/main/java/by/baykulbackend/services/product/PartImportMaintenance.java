package by.baykulbackend.services.product;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.imports.maintenance-enabled", havingValue = "true", matchIfMissing = true)
public class PartImportMaintenance implements ApplicationRunner {
    private final PartImportService imports;

    @Override
    public void run(ApplicationArguments args) {
        imports.recoverInterrupted();
        imports.cleanup();
    }

    @Scheduled(fixedDelay = 3600000, initialDelay = 3600000)
    public void cleanup() {
        imports.cleanup();
    }
}
