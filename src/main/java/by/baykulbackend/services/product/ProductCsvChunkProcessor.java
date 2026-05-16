package by.baykulbackend.services.product;

import by.baykulbackend.database.dao.finance.Currency;
import by.baykulbackend.database.dao.product.Part;
import by.baykulbackend.database.dto.product.CsvUploadResult;
import by.baykulbackend.database.dto.product.SkippedRow;
import by.baykulbackend.database.repository.product.IPartRepository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handles the transactional processing of individual CSV chunks.
 * Each chunk is processed in its own transaction ({@code REQUIRES_NEW})
 * to isolate failures and reduce lock contention on large imports.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductCsvChunkProcessor {
    private static final int EXPECTED_COLUMNS = 8;

    private final IPartRepository iPartRepository;
    private final EntityManager entityManager;

    /**
     * Processes a single chunk of CSV lines within its own transaction.
     * <p>
     * Performs the following phases:
     * <ol>
     *   <li>Extract unique articles from the chunk</li>
     *   <li>Fetch existing parts from DB using PostgreSQL array parameter</li>
     *   <li>Validate, parse, and separate new inserts from updates</li>
     *   <li>Batch-save inserts and updates separately for optimal batching</li>
     * </ol>
     *
     * @param lines           The list of CSV lines in the current chunk.
     * @param result          The upload result tracker to populate with counts and errors.
     * @param startLineNumber The starting line number in the original CSV (for error reporting).
     * @param chunkNumber     The sequential chunk number (for logging).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processChunk(List<String[]> lines, CsvUploadResult result,
                             int startLineNumber, int chunkNumber) {
        long chunkStartTime = System.currentTimeMillis();

        // Phase 1: Extract unique articles from the chunk
        long startExtract = System.currentTimeMillis();
        Set<String> articlesInChunk = lines.stream()
                .filter(line -> line.length >= 1)
                .map(line -> line[0])
                .collect(Collectors.toSet());
        log.trace("Chunk #{} - Phase 1: Extracted {} articles in {} ms", chunkNumber,
                articlesInChunk.size(), (System.currentTimeMillis() - startExtract));

        // Phase 2: Fetch existing parts using PostgreSQL array parameter (single bind)
        long startDbFetch = System.currentTimeMillis();
        Map<String, Part> existingParts = iPartRepository
                .findAllByArticleArray(articlesInChunk.toArray(new String[0]))
                .stream()
                .collect(Collectors.toMap(Part::getArticle, part -> part));
        log.trace("Chunk #{} - Phase 2: Fetched {} existing articles from DB in {} ms",
                chunkNumber, existingParts.size(), (System.currentTimeMillis() - startDbFetch));

        // Phase 3: Validate, parse, and separate inserts from updates
        long startValidation = System.currentTimeMillis();
        List<Part> newParts = new ArrayList<>();
        List<Part> updatedParts = new ArrayList<>();
        Map<String, Part> seenInChunk = new HashMap<>();

        int currentLineNumber = startLineNumber;
        for (String[] line : lines) {
            Optional<String> error = validateLine(line);
            if (error.isPresent()) {
                result.setSkipped(result.getSkipped() + 1);
                result.getSkippedDetails().add(
                        new SkippedRow(currentLineNumber, error.get(), String.join(";", line)));
                log.trace("Validation failed at line {}: {}", currentLineNumber, error.get());
            } else {
                String article = line[0];
                Part part;
                boolean isUpdate;

                if (existingParts.containsKey(article)) {
                    // Existing in DB → update
                    part = existingParts.get(article);
                    isUpdate = true;
                } else if (seenInChunk.containsKey(article)) {
                    // Duplicate within chunk → update the already-created part
                    part = seenInChunk.get(article);
                    isUpdate = true;
                } else {
                    // Brand new part
                    part = new Part();
                    isUpdate = false;
                }

                applyFieldsFromLine(part, line);
                seenInChunk.put(article, part);

                if (isUpdate) {
                    if (!updatedParts.contains(part)) {
                        updatedParts.add(part);
                    }
                    result.setUpdated(result.getUpdated() + 1);
                } else {
                    newParts.add(part);
                    result.setSaved(result.getSaved() + 1);
                }
            }
            currentLineNumber++;
        }
        log.trace("Chunk #{} - Phase 3: Validated and parsed {} lines in {} ms", chunkNumber,
                lines.size(), (System.currentTimeMillis() - startValidation));

        // Phase 4: Save to database — inserts and updates as separate contiguous batches
        long startSave = System.currentTimeMillis();
        if (!newParts.isEmpty()) {
            iPartRepository.saveAll(newParts);
        }
        if (!updatedParts.isEmpty()) {
            iPartRepository.saveAll(updatedParts);
        }
        if (!newParts.isEmpty() || !updatedParts.isEmpty()) {
            entityManager.flush();
            entityManager.clear();
        }
        log.trace("Chunk #{} - Phase 4: Saved {} new + {} updated parts in {} ms",
                chunkNumber, newParts.size(), updatedParts.size(),
                (System.currentTimeMillis() - startSave));

        long chunkTotalTime = System.currentTimeMillis() - chunkStartTime;
        log.debug("Chunk #{}: Processed {} lines, saved {} new + {} updated parts in {} ms",
                chunkNumber, lines.size(), newParts.size(), updatedParts.size(), chunkTotalTime);
    }

    /**
     * Applies all field values from a CSV line to a {@link Part} entity.
     * Handles type conversions, default values, and decimal formatting.
     *
     * @param part The part entity to populate.
     * @param line A single parsed row from the CSV.
     */
    private void applyFieldsFromLine(Part part, String[] line) {
        part.setArticle(line[0]);
        part.setName(line[1]);
        if (StringUtils.isNotBlank(line[2])) {
            part.setWeight(Double.parseDouble(line[2].replace(',', '.')));
        }
        part.setMinCount(StringUtils.isNotBlank(line[3]) ? Integer.parseInt(line[3]) : 1);
        if (StringUtils.isNotBlank(line[4])) {
            part.setStorageCount(Integer.parseInt(line[4]));
        }
        part.setReturnPart(StringUtils.isNotBlank(line[5])
                ? new BigDecimal(line[5].replace(',', '.')).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        part.setPrice(new BigDecimal(line[6].replace(',', '.')).setScale(2, RoundingMode.HALF_UP));
        part.setCurrency(Currency.EUR);
        part.setBrand(line[7]);
    }

    /**
     * Runs all validation rules on a single CSV line in short-circuit order.
     *
     * @param line The CSV line to validate.
     * @return An {@link Optional} containing the first error message, or empty if valid.
     */
    private Optional<String> validateLine(String[] line) {
        return validateStructure(line)
                .or(() -> validateRequiredFields(line))
                .or(() -> validateFieldLengths(line))
                .or(() -> validateNumericValues(line))
                .or(() -> validateDecimalValues(line));
    }

    /**
     * Validates that the CSV row contains the exact expected number of columns.
     */
    private Optional<String> validateStructure(String[] line) {
        return line.length != EXPECTED_COLUMNS
                ? Optional.of("Incorrect number of columns") : Optional.empty();
    }

    /**
     * Validates that mandatory fields (Article, Price, and Brand) are not blank.
     */
    private Optional<String> validateRequiredFields(String[] line) {
        if (StringUtils.isAnyEmpty(line[0], line[6], line[7])) {
            return Optional.of(
                    "Required fields are missing. Article, price, and brand must be provided.");
        }
        return Optional.empty();
    }

    /**
     * Validates that string fields do not exceed maximum database column lengths.
     */
    private Optional<String> validateFieldLengths(String[] line) {
        if (line[0].length() > 50) {
            return Optional.of("Article exceeds 50 characters.");
        }
        if (line[1].length() > 255) {
            return Optional.of("Name exceeds 255 characters.");
        }
        if (line[7].length() > 50) {
            return Optional.of("Brand exceeds 50 characters.");
        }
        return Optional.empty();
    }

    /**
     * Validates numeric constraints for Weight, Min Count, and Storage Count fields.
     */
    private Optional<String> validateNumericValues(String[] line) {
        try {
            if (StringUtils.isNotEmpty(line[2])
                    && Double.parseDouble(line[2].replace(',', '.')) < 0) {
                return Optional.of("Weight cannot be negative.");
            }
            if (StringUtils.isNotEmpty(line[3]) && Integer.parseInt(line[3]) < 1) {
                return Optional.of("Min count must be at least 1.");
            }
            if (StringUtils.isNotEmpty(line[4]) && Integer.parseInt(line[4]) < 0) {
                return Optional.of("Storage count cannot be negative.");
            }
        } catch (NumberFormatException e) {
            return Optional.of("Invalid number format in a numeric field.");
        }
        return Optional.empty();
    }

    /**
     * Validates format and constraints of BigDecimal fields (Return Part and Price).
     */
    private Optional<String> validateDecimalValues(String[] line) {
        try {
            if (StringUtils.isNotEmpty(line[5])) {
                BigDecimal returnPart = new BigDecimal(line[5].replace(',', '.'));
                if (returnPart.scale() > 2 || returnPart.compareTo(BigDecimal.ZERO) < 0) {
                    return Optional.of("Invalid format for return part.");
                }
            }
            BigDecimal price = new BigDecimal(line[6].replace(',', '.'));
            if (price.scale() > 2 || price.compareTo(BigDecimal.ZERO) < 0) {
                return Optional.of("Invalid format for price.");
            }
        } catch (NumberFormatException e) {
            return Optional.of("Invalid number format in a decimal field.");
        }
        return Optional.empty();
    }
}
