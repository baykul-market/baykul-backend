package by.baykulbackend.services.product;

import by.baykulbackend.database.dto.product.CsvUploadResult;
import by.baykulbackend.database.dto.product.ProductDto;
import by.baykulbackend.services.user.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/** Compatibility adapter for the synchronous legacy upload endpoint. */
@Service
@RequiredArgsConstructor
public class ProductCsvService {
    private final PartImportService imports;
    private final AuthService authService;

    public ResponseEntity<CsvUploadResult> parseParts(ProductDto request) {
        return imports.uploadLegacy(request.getCsvFile(), authService.getAuthInfo().getPrincipal().toString());
    }
}
