package by.baykulbackend.services.product;

import by.baykulbackend.database.dao.product.Part;
import by.baykulbackend.database.dto.product.CsvUploadResult;
import by.baykulbackend.database.dto.product.ProductDto;
import by.baykulbackend.database.repository.product.IPartRepository;
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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class ProductCsvServiceTest {

    @Mock
    private IPartRepository iPartRepository;

    @InjectMocks
    private ProductCsvService productCsvService;

    @Test
    void parsePartsAllValidShouldReturn200() {
        // Arrange
        String csvContent = "article;name;weight;min_count;storage_count;return_part;price;brand\n" +
                "art1;Name1;1.0;1;1;0.0;10.0;Brand1\n" +
                "art2;Name2;2.0;2;2;0.0;20.0;Brand2";
        MultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));
        ProductDto dto = new ProductDto();
        dto.setCsvFile(file);

        // art1 already exists
        Part existingPart = new Part();
        existingPart.setArticle("art1");
        when(iPartRepository.findAllByArticleIn(any())).thenReturn(Set.of(existingPart));

        // Act
        ResponseEntity<CsvUploadResult> response = productCsvService.parseParts(dto);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getSaved()); // art2
        assertEquals(1, response.getBody().getUpdated()); // art1
        assertEquals(0, response.getBody().getSkipped());
        assertTrue(response.getBody().getSkippedDetails().isEmpty());

        verify(iPartRepository, times(1)).saveAll(any());
    }

    @Test
    void parsePartsWithInvalidRowsShouldReturn207() {
        // Arrange
        String csvContent = "article;name;weight;min_count;storage_count;return_part;price;brand\n" +
                "art1;Name1;1.0;1;1;0.0;10.0;Brand1\n" +
                "art2;;1.0;1;1;0.0;;Brand2\n" + // Missing price -> invalid
                "art3;Name3;1.0;1;1;0.0;30.0;Brand3";
        MultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));
        ProductDto dto = new ProductDto();
        dto.setCsvFile(file);

        when(iPartRepository.findAllByArticleIn(any())).thenReturn(Collections.emptySet());

        // Act
        ResponseEntity<CsvUploadResult> response = productCsvService.parseParts(dto);

        // Assert
        assertEquals(HttpStatus.MULTI_STATUS, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().getSaved()); // art1, art3
        assertEquals(0, response.getBody().getUpdated());
        assertEquals(1, response.getBody().getSkipped()); // art2
        assertEquals(1, response.getBody().getSkippedDetails().size());
        assertEquals(3, response.getBody().getSkippedDetails().get(0).getRowNumber()); // line 3 (1 header, 2nd data row)
        assertTrue(response.getBody().getSkippedDetails().get(0).getErrorMessage().contains("Required fields"));
    }

    @Test
    void parsePartsEmptyNameShouldBeValidAndSaved() {
        // Arrange
        String csvContent = "article;name;weight;min_count;storage_count;return_part;price;brand\n" +
                "art1;;1.0;1;1;0.0;10.0;Brand1"; // Empty name
        MultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));
        ProductDto dto = new ProductDto();
        dto.setCsvFile(file);

        when(iPartRepository.findAllByArticleIn(any())).thenReturn(Collections.emptySet());

        // Act
        ResponseEntity<CsvUploadResult> response = productCsvService.parseParts(dto);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getSaved());
        assertEquals(0, response.getBody().getSkipped());
    }
}
