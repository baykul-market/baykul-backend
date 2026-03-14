package by.baykulbackend.services.product;

import by.baykulbackend.database.dao.finance.Currency;
import by.baykulbackend.database.dao.product.Part;
import by.baykulbackend.database.dto.product.ProductDto;
import by.baykulbackend.database.repository.product.IPartRepository;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service responsible for parsing and processing bulk product imports via CSV.
 * It handles chunked reading, validation, and batch saving to optimize performance
 * and memory usage for large datasets.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductCsvService {
    private static final char SEPARATOR = ';';
    private static final int EXPECTED_COLUMNS = 8;
    private static final int CHUNK_SIZE = 5000;

    private final IPartRepository iPartRepository;

    /**
     * The main entry point for processing the uploaded CSV file containing product parts.
     * Reads the file line-by-line and processes it in memory-efficient chunks.
     *
     * @param productDto Data Transfer Object containing the multipart CSV file.
     * @return A {@link ResponseEntity} containing a map of statuses and any row-specific validation errors.
     */
    @Transactional
    public ResponseEntity<?> parseParts(ProductDto productDto) {
        long processStartTime = System.currentTimeMillis();
        String fileName = productDto.getCsvFile().getOriginalFilename();
        log.info("Starting CSV processing for file: {}", fileName);

        Map<String, String> response = new HashMap<>();
        int totalLinesProcessed = 0;
        int totalPartsSaved = 0;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(productDto.getCsvFile().getInputStream(), StandardCharsets.UTF_8));
             CSVReader csvReader = new CSVReaderBuilder(br)
                     .withCSVParser(new CSVParserBuilder().withSeparator(SEPARATOR).withIgnoreQuotations(true).withIgnoreLeadingWhiteSpace(true).build())
                     .withSkipLines(1)
                     .build()) {

            List<String[]> lines = new ArrayList<>(CHUNK_SIZE);
            String[] line;
            int lineNumber = 2;
            int chunkNumber = 1;

            while ((line = csvReader.readNext()) != null) {
                lines.add(line);
                if (lines.size() >= CHUNK_SIZE) {
                    totalPartsSaved += processChunk(lines, response, lineNumber - lines.size(), chunkNumber);
                    totalLinesProcessed += lines.size();
                    lines.clear();
                    chunkNumber++;
                }
                lineNumber++;
            }
            if (!lines.isEmpty()) {
                totalPartsSaved += processChunk(lines, response, lineNumber - lines.size(), chunkNumber);
                totalLinesProcessed += lines.size();
            }

        } catch (IOException | CsvValidationException e) {
            log.error("Error while parsing CSV file '{}': {}", fileName, e.getMessage(), e);
            response.put("error", "Error while parsing CSV file: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        long totalTimeMs = System.currentTimeMillis() - processStartTime;
        log.info("CSV processing completed in {} ms. Total lines read: {}. Successfully saved: {}. Validation errors: {}",
                totalTimeMs, totalLinesProcessed, totalPartsSaved, response.size());

        response.put("status", "CSV processing finished.");
        return ResponseEntity.ok(response);
    }

    /**
     * Processes a single chunk of CSV lines. Extracts articles for bulk database validation,
     * filters out invalid lines, parses the valid ones into entities, and saves them in a batch.
     *
     * @param lines           The list of CSV lines in the current chunk.
     * @param response        The response map to populate with any validation errors.
     * @param startLineNumber The starting line number in the original CSV (for accurate error reporting).
     */
    private int processChunk(List<String[]> lines, Map<String, String> response, int startLineNumber, int chunkNumber) {
        long chunkStartTime = System.currentTimeMillis();

        // Phase 1: Extract articles from the chunk
        long startExtract = System.currentTimeMillis();
        Set<String> articlesInChunk = lines.stream().map(line -> line[0]).collect(Collectors.toSet());
        log.trace("Chunk #{} - Phase 1: Extracted {} articles in {} ms", chunkNumber, articlesInChunk.size(), (System.currentTimeMillis() - startExtract));

        // Phase 2: Fetch existing articles from DB
        long startDbFetch = System.currentTimeMillis();
        Map<String, Part> existingParts = iPartRepository.findAllByArticleIn(articlesInChunk)
                .stream()
                .collect(Collectors.toMap(
                        Part::getArticle,
                        part -> part
                ));
        log.trace("Chunk #{} - Phase 2: Fetched {} existing articles from DB in {} ms", chunkNumber, existingParts.size(), (System.currentTimeMillis() - startDbFetch));

        List<Part> partsToSave = new ArrayList<>();

        long startValidation = System.currentTimeMillis();
        int currentLineNumber = startLineNumber;
        for (String[] line : lines) {
            if (!isInvalidLine(line, response, currentLineNumber)) {
                Part part = parsePartFromLine(line, existingParts);
                partsToSave.add(part);
            }
            currentLineNumber++;
        }
        log.trace("Chunk #{} - Phase 3: Validated and parsed {} lines in {} ms", chunkNumber, lines.size(), (System.currentTimeMillis() - startValidation));

        // Phase 4: Save to Database
        if (!partsToSave.isEmpty()) {
            long startSave = System.currentTimeMillis();
            iPartRepository.saveAll(partsToSave);
            log.trace("Chunk #{} - Phase 4: Saved {} parts to DB using saveAll in {} ms", chunkNumber, partsToSave.size(), (System.currentTimeMillis() - startSave));
        }

        long chunkTotalTime = System.currentTimeMillis() - chunkStartTime;
        log.debug("Chunk #{}: Processed {} lines, saved {} parts in {} ms", chunkNumber, lines.size(), partsToSave.size(), chunkTotalTime);

        return partsToSave.size();
    }

    /**
     * Maps a valid array of CSV string values to a new {@link Part} entity.
     * Handles type conversions, default values, and standardizes decimal formatting.
     *
     * @param line A single parsed row from the CSV representing a product part.
     * @param existingParts Existing parts to update
     * @return A fully populated {@link Part} entity ready to be saved.
     */
    private Part parsePartFromLine(String[] line, Map<String, Part> existingParts) {
        Part part = existingParts.getOrDefault(line[0], new Part());
        part.setArticle(line[0]);
        part.setName(line[1]);
        if (StringUtils.isNotBlank(line[2])) {
            part.setWeight(Double.parseDouble(line[2].replace(',', '.')));
        }
        part.setMinCount(StringUtils.isNotBlank(line[3]) ? Integer.parseInt(line[3]) : 1);
        if (StringUtils.isNotBlank(line[4])) {
            part.setStorageCount(Integer.parseInt(line[4]));
        }
        part.setReturnPart(StringUtils.isNotBlank(line[5]) ? new BigDecimal(line[5].replace(',', '.')).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        part.setPrice(new BigDecimal(line[6].replace(',', '.')).setScale(2, RoundingMode.HALF_UP));
        part.setCurrency(Currency.EUR);
        part.setBrand(line[7]);
        return part;
    }

    /**
     * Orchestrates all validation rules for a single CSV line. If the line is invalid,
     * the specific error is appended to the response map using the row's line number.
     *
     * @param line                  The CSV line to validate.
     * @param response              The map-collecting validation errors.
     * @param lineNumber            The actual line number of the row in the CSV file.
     * @return {@code true} if the line violates any validation rule; {@code false} if it is valid.
     */
    private boolean isInvalidLine(String[] line, Map<String, String> response, int lineNumber) {
        Optional<String> error = validateStructure(line)
                .or(() -> validateRequiredFields(line))
                .or(() -> validateFieldLengths(line))
                .or(() -> validateNumericValues(line))
                .or(() -> validateDecimalValues(line));

        if (error.isPresent()) {
            String errorMessage = error.get();
            response.put("error_row_" + lineNumber, errorMessage);
            log.trace("Validation failed at line {}: {}", lineNumber, errorMessage);
            return true;
        }
        return false;
    }

    /**
     * Validates that the CSV row contains the exact expected number of columns.
     *
     * @param line The CSV line to validate.
     * @return An {@link Optional} containing an error message if invalid, or empty if valid.
     */
    private Optional<String> validateStructure(String[] line) {
        return line.length != EXPECTED_COLUMNS ? Optional.of("Incorrect number of columns") : Optional.empty();
    }

    /**
     * Validates that mandatory fields (Article, Name, Price, and Brand) are not blank.
     *
     * @param line The CSV line to validate.
     * @return An {@link Optional} containing an error message if any required field is empty, or empty if valid.
     */
    private Optional<String> validateRequiredFields(String[] line) {
        if (StringUtils.isAnyEmpty(line[0], line[1], line[6], line[7])) {
            return Optional.of("Required fields are missing. Article, name, price, and brand must be provided.");
        }
        return Optional.empty();
    }

    /**
     * Validates that the string fields do not exceed maximum database column lengths.
     *
     * @param line The CSV line to validate.
     * @return An {@link Optional} containing an error message if any string is too long, or empty if valid.
     */
    private Optional<String> validateFieldLengths(String[] line) {
        if (line[0].length() > 50) return Optional.of("Article exceeds 50 characters.");
        if (line[1].length() > 255) return Optional.of("Name exceeds 255 characters.");
        if (line[7].length() > 50) return Optional.of("Brand exceeds 50 characters.");
        return Optional.empty();
    }

    /**
     * Validates the numeric constraints for Weight, Min Count, and Storage Count fields.
     * Ensures they can be parsed correctly and do not contain forbidden negative values.
     *
     * @param line The CSV line to validate.
     * @return An {@link Optional} containing an error message if constraints are violated, or empty if valid.
     */
    private Optional<String> validateNumericValues(String[] line) {
        try {
            if (StringUtils.isNotEmpty(line[2]) && Double.parseDouble(line[2].replace(',', '.')) < 0) {
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
     * Validates the format and constraints of BigDecimal fields (Return Part and Price).
     * Ensures a proper scale (max 2 decimal places) and prevents negative pricing.
     *
     * @param line The CSV line to validate.
     * @return An {@link Optional} containing an error message if format or value constraints are violated, or empty if valid.
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
