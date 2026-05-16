package by.baykulbackend.services.product;

import by.baykulbackend.database.dao.product.Part;
import by.baykulbackend.database.dto.product.CsvUploadResult;
import by.baykulbackend.database.dto.product.ProductDto;
import by.baykulbackend.database.dto.product.SkippedRow;
import by.baykulbackend.database.repository.product.IPartRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class ProductCsvServiceTest {

    @Mock
    private IPartRepository iPartRepository;

    @Mock
    private EntityManager entityManager;

    @Mock
    private ProductCsvChunkProcessor chunkProcessor;

    @InjectMocks
    private ProductCsvService productCsvService;

    @InjectMocks
    private ProductCsvChunkProcessor chunkProcessorReal;

    @Test
    void parsePartsAllValidShouldReturn200() {
        // Arrange
        String csvContent = "article;name;weight;min_count;storage_count;return_part;price;brand\n" +
                "art1;Name1;1.0;1;1;0.0;10.0;Brand1\n" +
                "art2;Name2;2.0;2;2;0.0;20.0;Brand2";
        MultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8));
        ProductDto dto = new ProductDto();
        dto.setCsvFile(file);

        // art1 already exists
        Part existingPart = new Part();
        existingPart.setArticle("art1");

        // Simulate chunk processor behavior:
        // - finds art1 in DB → update, art2 not found → save
        doAnswer(invocation -> {
            List<String[]> lines = invocation.getArgument(0);
            CsvUploadResult result = invocation.getArgument(1);
            result.setSaved(1);   // art2
            result.setUpdated(1); // art1
            return null;
        }).when(chunkProcessor).processChunk(anyList(), any(CsvUploadResult.class),
                anyInt(), anyInt());

        // Act
        ResponseEntity<CsvUploadResult> response = productCsvService.parseParts(dto);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getSaved()); // art2
        assertEquals(1, response.getBody().getUpdated()); // art1
        assertEquals(0, response.getBody().getSkipped());
        assertTrue(response.getBody().getSkippedDetails().isEmpty());

        verify(chunkProcessor, times(1)).processChunk(anyList(),
                any(CsvUploadResult.class), anyInt(), anyInt());
    }

    @Test
    void parsePartsWithInvalidRowsShouldReturn207() {
        // Arrange
        String csvContent = "article;name;weight;min_count;storage_count;return_part;price;brand\n" +
                "art1;Name1;1.0;1;1;0.0;10.0;Brand1\n" +
                "art2;;1.0;1;1;0.0;;Brand2\n" + // Missing price -> invalid
                "art3;Name3;1.0;1;1;0.0;30.0;Brand3";
        MultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8));
        ProductDto dto = new ProductDto();
        dto.setCsvFile(file);

        // Simulate chunk processor: 2 saved, 1 skipped
        doAnswer(invocation -> {
            CsvUploadResult result = invocation.getArgument(1);
            result.setSaved(2);   // art1, art3
            result.setSkipped(1); // art2
            result.getSkippedDetails().add(
                    new SkippedRow(3, "Required fields are missing. "
                            + "Article, price, and brand must be provided.",
                            "art2;;1.0;1;1;0.0;;Brand2"));
            return null;
        }).when(chunkProcessor).processChunk(anyList(), any(CsvUploadResult.class),
                anyInt(), anyInt());

        // Act
        ResponseEntity<CsvUploadResult> response = productCsvService.parseParts(dto);

        // Assert
        assertEquals(HttpStatus.MULTI_STATUS, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().getSaved()); // art1, art3
        assertEquals(0, response.getBody().getUpdated());
        assertEquals(1, response.getBody().getSkipped()); // art2
        assertEquals(1, response.getBody().getSkippedDetails().size());
        assertEquals(3, response.getBody().getSkippedDetails().get(0).getRowNumber());
        assertTrue(response.getBody().getSkippedDetails().get(0).getErrorMessage()
                .contains("Required fields"));
    }

    @Test
    void parsePartsEmptyNameShouldBeValidAndSaved() {
        // Arrange
        String csvContent = "article;name;weight;min_count;storage_count;return_part;price;brand\n" +
                "art1;;1.0;1;1;0.0;10.0;Brand1"; // Empty name
        MultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8));
        ProductDto dto = new ProductDto();
        dto.setCsvFile(file);

        doAnswer(invocation -> {
            CsvUploadResult result = invocation.getArgument(1);
            result.setSaved(1);
            return null;
        }).when(chunkProcessor).processChunk(anyList(), any(CsvUploadResult.class),
                anyInt(), anyInt());

        // Act
        ResponseEntity<CsvUploadResult> response = productCsvService.parseParts(dto);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getSaved());
        assertEquals(0, response.getBody().getSkipped());
    }

    // ---- Tests for the chunk processor itself ----

    @Test
    void chunkProcessorShouldSeparateInsertsAndUpdates() {
        // Arrange
        Part existingPart = new Part();
        existingPart.setArticle("art1");

        when(iPartRepository.findAllByArticleArray(any(String[].class)))
                .thenReturn(List.of(existingPart));

        List<String[]> lines = List.of(
                new String[]{"art1", "Name1", "1.0", "1", "1", "0.0", "10.0", "Brand1"},
                new String[]{"art2", "Name2", "2.0", "2", "2", "0.0", "20.0", "Brand2"}
        );

        CsvUploadResult result = new CsvUploadResult();

        // Act
        chunkProcessorReal.processChunk(lines, result, 2, 1);

        // Assert
        assertEquals(1, result.getSaved());   // art2 is new
        assertEquals(1, result.getUpdated()); // art1 exists
        assertEquals(0, result.getSkipped());
        // saveAll called twice: once for inserts, once for updates
        verify(iPartRepository, times(2)).saveAll(any());
    }

    @Test
    void chunkProcessorShouldHandleDuplicateArticlesInChunk() {
        // Arrange — both art1 occurrences are new (not in DB)
        when(iPartRepository.findAllByArticleArray(any(String[].class)))
                .thenReturn(Collections.emptyList());

        List<String[]> lines = List.of(
                new String[]{"art1", "Name1", "1.0", "1", "1", "0.0", "10.0", "Brand1"},
                new String[]{"art1", "Name1-Updated", "2.0", "2", "2", "0.0", "20.0", "Brand2"}
        );

        CsvUploadResult result = new CsvUploadResult();

        // Act
        chunkProcessorReal.processChunk(lines, result, 2, 1);

        // Assert
        assertEquals(1, result.getSaved());   // first occurrence counts as saved
        assertEquals(1, result.getUpdated()); // second occurrence updates the same Part
        assertEquals(0, result.getSkipped());
    }

    @Test
    void chunkProcessorShouldSkipInvalidRows() {
        // Arrange
        when(iPartRepository.findAllByArticleArray(any(String[].class)))
                .thenReturn(Collections.emptyList());

        List<String[]> lines = List.of(
                new String[]{"art1", "Name1", "1.0", "1", "1", "0.0", "10.0", "Brand1"},
                new String[]{"art2", "", "1.0", "1", "1", "0.0", "", "Brand2"}, // missing price
                new String[]{"art3", "Name3", "1.0", "1", "1", "0.0", "30.0", "Brand3"}
        );

        CsvUploadResult result = new CsvUploadResult();

        // Act
        chunkProcessorReal.processChunk(lines, result, 2, 1);

        // Assert
        assertEquals(2, result.getSaved());   // art1, art3
        assertEquals(0, result.getUpdated());
        assertEquals(1, result.getSkipped()); // art2
        assertEquals(1, result.getSkippedDetails().size());
        assertTrue(result.getSkippedDetails().get(0).getErrorMessage()
                .contains("Required fields"));
    }
}
