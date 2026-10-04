package by.baykulbackend.services.product;

import by.baykulbackend.database.dto.product.CsvUploadResult;
import by.baykulbackend.database.dto.product.ProductDto;
import by.baykulbackend.security.JwtAuthentication;
import by.baykulbackend.services.user.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

/** Row validation and transactional import behavior are covered by parser and PostgreSQL integration tests. */
class ProductCsvServiceTest {
    @Test
    void delegatesTheLegacyFileAndAuthenticatedUploaderWithoutChangingTheResponse() {
        var imports = mock(PartImportService.class);
        var auth = mock(AuthService.class);
        var authentication = new JwtAuthentication();
        authentication.setLogin("manager");
        when(auth.getAuthInfo()).thenReturn(authentication);
        var request = new ProductDto();
        var file = new MockMultipartFile("csvFile", "parts.csv", "text/csv", "header".getBytes());
        request.setCsvFile(file);
        var expected = ResponseEntity.status(207).body(new CsvUploadResult());
        when(imports.uploadLegacy(file, "manager")).thenReturn(expected);
        assertSame(expected, new ProductCsvService(imports, auth).parseParts(request));
    }
}
