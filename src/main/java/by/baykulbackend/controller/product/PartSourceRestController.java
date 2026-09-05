package by.baykulbackend.controller.product;

import by.baykulbackend.database.dao.product.PartSource;
import by.baykulbackend.database.dto.product.PartDto;
import by.baykulbackend.database.dto.product.PartImportView;
import by.baykulbackend.database.dto.product.PartSourceView;
import by.baykulbackend.database.dto.product.SkippedRow;
import by.baykulbackend.services.product.PartImportService;
import by.baykulbackend.services.product.PartImportStore;
import by.baykulbackend.services.product.PartService;
import by.baykulbackend.services.product.PartSourceService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/product/sources")
@PreAuthorize("hasAuthority('products:write')")
@RequiredArgsConstructor
@Tag(name = "Part sources", description = "Manager source lifecycle and staged, explicitly confirmed catalog imports")
public class PartSourceRestController {
    public record CreateSource(@NotBlank @Size(max = 255) String name) { }
    public record UpdateSource(@Size(max = 255) String name, PartSource.Status status) { }
    public record ApplyImport(boolean acceptSkippedRows) { }

    private final PartSourceService sources;
    private final PartImportService imports;
    private final PartImportStore store;
    private final PartService parts;

    @GetMapping
    public Page<PartSourceView> list(@RequestParam(defaultValue = "") String text,
                                    @RequestParam(required = false) PartSource.Status status,
                                    @PageableDefault(size = 20) Pageable page) {
        return sources.list(text, status, bounded(page));
    }

    @PostMapping
    public ResponseEntity<PartSourceView> create(@Valid @RequestBody CreateSource request) {
        return ResponseEntity.status(201).body(sources.create(request.name()));
    }

    @GetMapping("/{sourceId}")
    public PartSourceView get(@PathVariable UUID sourceId) {
        return sources.get(sourceId);
    }

    @PatchMapping("/{sourceId}")
    public PartSourceView update(@PathVariable UUID sourceId, @Valid @RequestBody UpdateSource request) {
        return sources.update(sourceId, request.name(), request.status());
    }

    @DeleteMapping("/{sourceId}")
    public PartSourceView archive(@PathVariable UUID sourceId) {
        return sources.archive(sourceId);
    }

    @GetMapping("/{sourceId}/parts")
    public Page<PartDto> parts(@PathVariable UUID sourceId, @RequestParam(defaultValue = "") String text,
                               @PageableDefault(size = 50) Pageable page) {
        sources.get(sourceId);
        return parts.getSourceParts(sourceId, text, bounded(page));
    }

    @GetMapping("/{sourceId}/imports")
    public Page<PartImportView> history(@PathVariable UUID sourceId, @PageableDefault(size = 20) Pageable page) {
        sources.get(sourceId);
        return store.history(sourceId, bounded(page));
    }

    @PostMapping(value = "/{sourceId}/imports", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PartImportView> upload(@PathVariable UUID sourceId, @RequestParam MultipartFile csvFile, Principal principal) {
        return ResponseEntity.accepted().body(imports.upload(sourceId, csvFile, principal.getName()));
    }

    @GetMapping("/{sourceId}/imports/{importId}")
    public PartImportView status(@PathVariable UUID sourceId, @PathVariable UUID importId) {
        return store.get(sourceId, importId);
    }

    @GetMapping("/{sourceId}/imports/{importId}/errors")
    public Page<SkippedRow> errors(@PathVariable UUID sourceId, @PathVariable UUID importId,
                                   @PageableDefault(size = 50) Pageable page) {
        store.get(sourceId, importId);
        return store.errors(importId, bounded(page));
    }

    @PostMapping("/{sourceId}/imports/{importId}/apply")
    public ResponseEntity<PartImportView> apply(@PathVariable UUID sourceId, @PathVariable UUID importId,
                                               @RequestBody ApplyImport request) {
        return ResponseEntity.accepted().body(imports.apply(sourceId, importId, request.acceptSkippedRows()));
    }

    @PostMapping("/{sourceId}/imports/{importId}/recheck")
    public PartImportView recheck(@PathVariable UUID sourceId, @PathVariable UUID importId) {
        return imports.recheck(sourceId, importId);
    }

    @PostMapping("/{sourceId}/imports/{importId}/cancel")
    public PartImportView cancel(@PathVariable UUID sourceId, @PathVariable UUID importId) {
        return imports.cancel(sourceId, importId);
    }

    private Pageable bounded(Pageable page) {
        return PageRequest.of(page.getPageNumber(), Math.min(page.getPageSize(), 100),
                org.springframework.data.domain.Sort.by("createdTs").descending().and(org.springframework.data.domain.Sort.by("id")));
    }
}
