package by.baykulbackend.services.product;

import by.baykulbackend.database.dto.product.CsvUploadResult;
import by.baykulbackend.database.dto.product.ProductDto;
import by.baykulbackend.database.dto.product.SkippedRow;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Service responsible for parsing and processing bulk product imports via CSV.
 * It handles chunked reading, validation, and batch saving to optimize
 * performance
 * and memory usage for large datasets.
 *
 * <p>Chunk processing is delegated to {@link ProductCsvChunkProcessor},
 * which runs each chunk in its own transaction for failure isolation
 * and reduced lock contention.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductCsvService {
    private static final char SEPARATOR = ';';
    private static final int CHUNK_SIZE = 5000;

    private final ProductCsvChunkProcessor chunkProcessor;

    /**
     * The main entry point for processing the uploaded CSV file containing product
     * parts.
     * Reads the file line-by-line and processes it in memory-efficient chunks.
     *
     * @param productDto Data Transfer Object containing the multipart CSV file.
     * @return A {@link ResponseEntity} containing a map of statuses and any
     *         row-specific validation errors.
     */
    public ResponseEntity<CsvUploadResult> parseParts(ProductDto productDto) {
        long processStartTime = System.currentTimeMillis();
        String fileName = productDto.getCsvFile().getOriginalFilename();
        log.info("Starting CSV processing for file: {}", fileName);

        CsvUploadResult result = new CsvUploadResult();
        int totalLinesProcessed = 0;

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(productDto.getCsvFile().getInputStream(), StandardCharsets.UTF_8));
                CSVReader csvReader = new CSVReaderBuilder(br)
                        .withCSVParser(new CSVParserBuilder().withSeparator(SEPARATOR).withIgnoreQuotations(true)
                                .withIgnoreLeadingWhiteSpace(true).build())
                        .withSkipLines(1)
                        .build()) {

            List<String[]> lines = new ArrayList<>(CHUNK_SIZE);
            String[] line;
            int lineNumber = 2;
            int chunkNumber = 1;

            while ((line = csvReader.readNext()) != null) {
                lines.add(line);
                if (lines.size() >= CHUNK_SIZE) {
                    chunkProcessor.processChunk(lines, result,
                            lineNumber - lines.size(), chunkNumber);
                    totalLinesProcessed += lines.size();
                    lines.clear();
                    chunkNumber++;
                }
                lineNumber++;
            }
            if (!lines.isEmpty()) {
                chunkProcessor.processChunk(lines, result,
                        lineNumber - lines.size(), chunkNumber);
                totalLinesProcessed += lines.size();
            }

        } catch (IOException | CsvValidationException e) {
            log.error("Error while parsing CSV file '{}': {}", fileName, e.getMessage(), e);
            result.getSkippedDetails().add(
                    new SkippedRow(0, "Error while parsing CSV file: " + e.getMessage(), ""));
            return new ResponseEntity<>(result, HttpStatus.BAD_REQUEST);
        }

        long totalTimeMs = System.currentTimeMillis() - processStartTime;
        log.info(
                "CSV processing completed in {} ms. Total lines read: {}. "
                        + "Successfully saved: {}. Updated: {}. Skipped: {}",
                totalTimeMs, totalLinesProcessed, result.getSaved(),
                result.getUpdated(), result.getSkipped());

        if (result.getSkipped() > 0) {
            return ResponseEntity.status(HttpStatus.MULTI_STATUS).body(result);
        }
        return ResponseEntity.ok(result);
    }
}
