package by.baykulbackend.services.product;

import by.baykulbackend.database.dao.product.Currency;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductCsvService {
    private static final char SEPARATOR = ';';
    private static final int EXPECTED_COLUMNS = 8;
    private static final int CHUNK_SIZE = 5000;

    private final IPartRepository iPartRepository;

    @Transactional
    public ResponseEntity<?> parseParts(ProductDto productDto) {
        Map<String, String> response = new HashMap<>();
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
                    processChunk(lines, response, lineNumber - lines.size());
                    lines.clear();
                    log.info("Processed chunk #{}", chunkNumber++);
                }
                lineNumber++;
            }
            if (!lines.isEmpty()) {
                processChunk(lines, response, lineNumber - lines.size());
                log.info("Processed final chunk.");
            }
        } catch (IOException | CsvValidationException e) {
            log.error("Error while parsing CSV file: {}", e.getMessage());
            response.put("error", "Error while parsing CSV file: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        response.put("status", "CSV processing finished.");
        return ResponseEntity.ok(response);
    }

    private void processChunk(List<String[]> lines, Map<String, String> response, int startLineNumber) {
        Set<String> articlesInChunk = lines.stream().map(line -> line[0]).collect(Collectors.toSet());
        Set<String> existingArticles = iPartRepository.findAllByArticleIn(articlesInChunk);
        Set<String> uniqueArticlesInChunk = new HashSet<>();
        List<Part> partsToSave = new ArrayList<>();

        int currentLineNumber = startLineNumber;
        for (String[] line : lines) {
            if (!isInvalidLine(line, response, currentLineNumber, existingArticles, uniqueArticlesInChunk)) {
                Part part = parsePartFromLine(line);
                partsToSave.add(part);
                uniqueArticlesInChunk.add(part.getArticle());
            }
            currentLineNumber++;
        }
        if (!partsToSave.isEmpty()) {
            iPartRepository.saveAll(partsToSave);
        }
    }

    private Part parsePartFromLine(String[] line) {
        Part part = new Part();
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

    private boolean isInvalidLine(String[] line, Map<String, String> response, int lineNumber, Set<String> existingArticles, Set<String> uniqueArticlesInChunk) {
        Optional<String> error = validateStructure(line)
                .or(() -> validateRequiredFields(line))
                .or(() -> validateUniqueness(line[0], existingArticles, uniqueArticlesInChunk))
                .or(() -> validateFieldLengths(line))
                .or(() -> validateNumericValues(line))
                .or(() -> validateDecimalValues(line));

        error.ifPresent(errorMessage -> response.put("error_row_" + lineNumber, errorMessage));
        return error.isPresent();
    }

    private Optional<String> validateStructure(String[] line) {
        return line.length != EXPECTED_COLUMNS ? Optional.of("Incorrect number of columns") : Optional.empty();
    }

    private Optional<String> validateRequiredFields(String[] line) {
        if (StringUtils.isAnyEmpty(line[0], line[1], line[6], line[7])) {
            return Optional.of("Required fields are missing. Article, name, price, and brand must be provided.");
        }
        return Optional.empty();
    }

    private Optional<String> validateUniqueness(String article, Set<String> existingArticles, Set<String> uniqueArticlesInChunk) {
        if (existingArticles.contains(article) || uniqueArticlesInChunk.contains(article)) {
            return Optional.of("Duplicate article " + article);
        }
        return Optional.empty();
    }

    private Optional<String> validateFieldLengths(String[] line) {
        if (line[0].length() > 50) return Optional.of("Article exceeds 50 characters.");
        if (line[1].length() > 255) return Optional.of("Name exceeds 255 characters.");
        if (line[7].length() > 50) return Optional.of("Brand exceeds 50 characters.");
        return Optional.empty();
    }

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
