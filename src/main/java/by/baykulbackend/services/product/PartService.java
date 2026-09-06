package by.baykulbackend.services.product;

import by.baykulbackend.database.dao.finance.Currency;
import by.baykulbackend.database.dao.product.Part;
import by.baykulbackend.database.dao.product.PartSource;
import by.baykulbackend.exceptions.CatalogConflictException;
import org.springframework.transaction.annotation.Transactional;
import by.baykulbackend.database.dao.user.User;
import by.baykulbackend.database.dto.product.PartByArticlesRequestDto;
import by.baykulbackend.database.dto.product.PartByArticlesResponseDto;
import by.baykulbackend.database.dto.product.PartDto;
import by.baykulbackend.database.model.Permission;
import by.baykulbackend.database.repository.product.IPartRepository;
import by.baykulbackend.database.repository.user.IUserRepository;
import by.baykulbackend.exceptions.NotFoundException;
import by.baykulbackend.services.finance.PriceService;
import by.baykulbackend.services.user.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;
import java.util.Collections;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PartService {
    private final PartCatalogGuard catalogGuard;
    private final IPartRepository iPartRepository;
    private final IUserRepository iUserRepository;
    private final AuthService authService;
    private final PriceService priceService;

    /**
     * Creates a new part in the system.
     *
     * @param part the Part object to create
     * @return ResponseEntity with success/error message
     */
    @Transactional
    public ResponseEntity<?> createPart(Part part) {
        Map<String, String> response = new HashMap<>();

        if (isNotValidNewPart(part, response)) {
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        part.setSource(catalogGuard.writableSource(PartSource.MANUAL_ID));
        if (hasNotUniqueData(part, response)) {
            return new ResponseEntity<>(response, HttpStatus.CONFLICT);
        }

        if (part.getMinCount() == null) {
            part.setMinCount(1);
        }

        if (part.getReturnPart() == null) {
            part.setReturnPart(new BigDecimal("0.00"));
        }

        if (part.getPrice() == null) {
            part.setPrice(new BigDecimal("0.00"));
        }

        if (part.getCurrency() == null) {
            part.setCurrency(Currency.EUR);
        }

        iPartRepository.save(part);
        response.put("create_part", "true");
        response.put("id", part.getId().toString());
        log.info("Part {} has ben created. -> {}", part.getArticle(), authService.getAuthInfo().getPrincipal());

        return ResponseEntity.ok(response);
    }

    /**
     * Get all parts with pagination
     *
     * @param pageable pagination parameters
     * @return Page of PartDto
     */
    public Page<PartDto> getAllParts(Pageable pageable) {
        Page<Part> partPage = iPartRepository.findAll(PartQueries.available(), pageable);
        return partPage.map(this::convertToDto);
    }

    /**
     * Get part by ID
     *
     * @param id part UUID
     * @return PartDto
     * @throws NotFoundException if part not found
     */
    public PartDto getPartById(UUID id) {
        Part part = iPartRepository.findById(id).orElseThrow(() -> new NotFoundException("Part not found"));
        if (!part.isAvailable() && !canManage()) {
            throw new NotFoundException("Part not found");
        }
        return convertToDto(part);
    }

    /**
     * Get part by exact article
     */
    public PartDto getPartByArticle(String article) {
        List<Part> parts = iPartRepository.findAll(PartQueries.available().and(PartQueries.exact("article", article)));
        if (parts.isEmpty()) {
            throw new NotFoundException("Part not found");
        }
        if (parts.size() > 1) {
            throw new CatalogConflictException("AMBIGUOUS_PART", "Multiple offers match this article; select a part by UUID");
        }
        return convertToDto(parts.getFirst());
    }

    /**
     * Get parts by exact name with pagination
     */
    public Page<PartDto> getPartsByExactName(String name, Pageable pageable) {
        return iPartRepository.findAll(PartQueries.available().and(PartQueries.exact("name", name)), pageable).map(this::convertToDto);
    }

    /**
     * Get parts by exact brand with pagination
     */
    public Page<PartDto> getPartsByExactBrand(String brand, Pageable pageable) {
        return iPartRepository.findAll(PartQueries.available().and(PartQueries.exact("brand", brand)), pageable).map(this::convertToDto);
    }

    /**
     * Searches for parts by article containing text.
     */
    public Page<PartDto> searchPartsByArticle(String article, Pageable pageable) {
        return iPartRepository.findAll(PartQueries.available().and(PartQueries.contains("article", article)), pageable)
                .map(this::convertToDto);
    }

    /**
     * Searches for parts by name containing text.
     */
    public Page<PartDto> searchPartsByName(String name, Pageable pageable) {
        return iPartRepository.findAll(PartQueries.available().and(PartQueries.contains("name", name)), pageable)
                .map(this::convertToDto);
    }

    /**
     * Searches for parts by brand containing text.
     */
    public Page<PartDto> searchPartsByBrand(String brand, Pageable pageable) {
        return iPartRepository.findAll(PartQueries.available().and(PartQueries.contains("brand", brand)), pageable)
                .map(this::convertToDto);
    }

    /**
     * Updates an existing part's information.
     * Only updates non-null fields from the provided part object.
     *
     * @param id   the UUID of the part to update
     * @param part the Part object containing updated fields
     * @return ResponseEntity with success/error message
     * @throws NotFoundException if no part is found with the given ID
     */
    @SuppressWarnings("BigDecimalMethodWithoutRoundingCalled")
    @Transactional
    public ResponseEntity<?> updatePart(UUID id, Part part) {
        Map<String, String> response = new HashMap<>();
        Part partFromDB = iPartRepository.findById(id).orElseThrow(() -> new NotFoundException("Part not found"));

        if (isNotValidPart(part, response)) {
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        catalogGuard.refreshForEdit(partFromDB);
        String nextArticle = StringUtils.isNotBlank(part.getArticle()) ? part.getArticle() : partFromDB.getArticle();
        String nextBrand = StringUtils.isNotBlank(part.getBrand()) ? part.getBrand() : partFromDB.getBrand();
        Optional<Part> conflict = iPartRepository.findBySourceIdAndArticleAndBrand(
                partFromDB.getSource().getId(), nextArticle, nextBrand);
        if (conflict.isPresent() && !conflict.get().getId().equals(id)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error_article", "Article and brand already exist in this source"));
        }

        if (StringUtils.isNotBlank(part.getArticle())) {
            partFromDB.setArticle(part.getArticle());
            log.info("Part's article with id {} has been updated -> {}",
                    id, authService.getAuthInfo().getPrincipal());
        }

        if (StringUtils.isNotBlank(part.getName())) {
            partFromDB.setName(part.getName());
            log.info("Part's name with id {} has been updated -> {}",
                    id, authService.getAuthInfo().getPrincipal());
        }

        if (part.getWeight() != null) {
            partFromDB.setWeight(part.getWeight());
            log.info("Part's weight with id {} has been updated -> {}",
                    id, authService.getAuthInfo().getPrincipal());
        }

        if (part.getMinCount() != null) {
            partFromDB.setMinCount(part.getMinCount());
            log.info("Part's minimum count with id {} has been updated -> {}",
                    id, authService.getAuthInfo().getPrincipal());
        }

        if (part.getStorageCount() != null) {
            partFromDB.setStorageCount(part.getStorageCount());
            log.info("Part's storage count with id {} has been updated -> {}",
                    id, authService.getAuthInfo().getPrincipal());
        }

        if (part.getReturnPart() != null) {
            partFromDB.setReturnPart(part.getReturnPart().setScale(2));
            log.info("Part's return part with id {} has been updated -> {}",
                    id, authService.getAuthInfo().getPrincipal());
        }

        if (part.getPrice() != null) {
            partFromDB.setPrice(part.getPrice().setScale(2));
            log.info("Part's price with id {} has been updated -> {}",
                    id, authService.getAuthInfo().getPrincipal());
        }

        if (part.getCurrency() != null) {
            partFromDB.setCurrency(part.getCurrency());
            log.info("Part's currency with id {} has been updated -> {}",
                    id, authService.getAuthInfo().getPrincipal());
        }

        if (StringUtils.isNotBlank(part.getBrand())) {
            partFromDB.setBrand(part.getBrand());
            log.info("Part's brand with id {} has been updated -> {}",
                    id, authService.getAuthInfo().getPrincipal());
        }

        iPartRepository.save(partFromDB);
        response.put("update_part", "true");

        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a part by their ID.
     *
     * @param id the UUID of the part to delete
     * @return ResponseEntity with success/error message
     * @throws NotFoundException if no part is found with the given ID
     */
    @Transactional
    public ResponseEntity<?> deletePartById(UUID id) {
        Map<String, String> response = new HashMap<>();
        Part partFromDB = iPartRepository.findById(id).orElseThrow(() -> new NotFoundException("Part not found"));

        catalogGuard.refreshForEdit(partFromDB);
        partFromDB.setCatalogPresent(false);
        iPartRepository.save(partFromDB);
        response.put("delete_part", "true");
        log.info("Delete part with id = {} -> {}", id, authService.getAuthInfo().getPrincipal());

        return ResponseEntity.ok(response);
    }

    /**
     * Searches for parts by article or name containing the given text.
     * Performs case-insensitive search.
     * Returns distinct parts to avoid duplicates.
     *
     * @param text the search text to match against part attributes
     * @param pageable pagination and sorting parameters
     * @return Page of matching PartDto objects
     */
    public Page<PartDto> searchPart(String text, Pageable pageable) {
        return iPartRepository.findAll(PartQueries.available().and(PartQueries.text(text)), pageable)
                .map(this::convertToDto);
    }

    /**
     * Searches for parts by multiple filters with pagination.
     *
     * @param article article filter (optional)
     * @param name name filter (optional)
     * @param brand brand filter (optional)
     * @param pageable pagination and sorting parameters
     * @return Page of matching PartDto objects
     */
    public Page<PartDto> searchPartsByFilter(String article, String name, String brand, Pageable pageable) {
        return iPartRepository.findAll(PartQueries.available()
                .and(PartQueries.contains("article", article))
                .and(PartQueries.contains("name", name))
                .and(PartQueries.contains("brand", brand)), pageable).map(this::convertToDto);
    }

    public PartByArticlesResponseDto getPartsByArticles(PartByArticlesRequestDto request) {
        if (request == null || request.getArticles() == null || request.getArticles().isEmpty()) {
            return PartByArticlesResponseDto.builder()
                    .parts(Collections.emptyList())
                    .build();
        }

        List<String> inputArticles = request.getArticles();
        Set<String> uniqueArticles = new HashSet<>(inputArticles);
        User user = getCurrentUser();

        Map<String, List<PartDto>> partMap = iPartRepository.findAll(PartQueries.available()
                        .and((root, query, cb) -> root.get("article").in(uniqueArticles))).stream()
                .sorted(java.util.Comparator.comparing(Part::getBrand).thenComparing(Part::getId))
                .map(part -> convertToDto(part, user))
                .collect(Collectors.groupingBy(PartDto::getArticle));
        List<PartDto> orderedParts = inputArticles.stream()
                .flatMap(article -> partMap.getOrDefault(article, List.of()).stream()).toList();

        return PartByArticlesResponseDto.builder()
                .parts(orderedParts)
                .build();
    }

    /**
     * Converts Part into PartDto
     */
    private PartDto convertToDto(Part part) {
        return convertToDto(part, getCurrentUser());
    }

    private User getCurrentUser() {
        return iUserRepository.findByLogin(authService.getAuthInfo().getPrincipal().toString())
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    /**
     * Converts Part into PartDto using the provided user context
     */
    private PartDto convertToDto(Part part, User user) {
        BigDecimal price = priceService.calculateProductPrice(
                part,
                part.getStorageCount() == null || part.getStorageCount() == 0,
                user
        );
        Currency currency = priceService.getSystemCurrency();

        PartDto partDto = PartDto.builder()
                .id(part.getId())
                .available(part.isAvailable())
                .createdTs(part.getCreatedTs())
                .updatedTs(part.getUpdatedTs())
                .article(part.getArticle())
                .name(part.getName())
                .weight(part.getWeight())
                .minCount(part.getMinCount())
                .storageCount(part.getStorageCount())
                .returnPart(part.getReturnPart())
                .price(price)
                .currency(currency)
                .brand(part.getBrand())
                .build();

        if (user.getRole().getPermissions().contains(Permission.PRODUCT_WRITE)) {
            partDto.setSourceId(part.getSource() == null ? null : part.getSource().getId());
            partDto.setSourceName(part.getSource() == null ? null : part.getSource().getName());
            partDto.setRealPrice(part.getPrice());
            partDto.setRealCurrency(part.getCurrency());
        }

        return partDto;
    }

    /**
     * Validates a new part request.
     *
     * @param part the Part object to validate
     * @param response Map to collect validation error messages
     * @return true if part is not valid for creation, false otherwise
     */
    private boolean isNotValidNewPart(Part part, Map<String, String> response) {
        if (StringUtils.isBlank(part.getArticle())) {
            response.put("error_article", "The article must not be empty");
            log.warn("The article must not be empty");
            return true;
        }

        if (StringUtils.isBlank(part.getName())) {
            response.put("error_name", "The name must not be empty");
            log.warn("The name must not be empty");
            return true;
        }

        if (StringUtils.isBlank(part.getBrand())) {
            response.put("error_brand", "The brand must not be empty");
            log.warn("The brand must not be empty");
            return true;
        }

        return isNotValidPart(part, response);
    }

    /**
     * Validates a part request for both creation and update.
     *
     * @param part the Part object to validate
     * @param response Map to collect validation error messages
     * @return true if part is not valid, false otherwise
     */
    private boolean isNotValidPart(Part part, Map<String, String> response) {
        if (StringUtils.isNotBlank(part.getArticle()) && part.getArticle().length() > 50) {
            response.put("error_article", "The article length must be less than 50");
            log.warn("The article length must be less than 50");
            return true;
        }

        if (StringUtils.isNotBlank(part.getName()) && part.getName().length() > 255) {
            response.put("error_name", "The name length must be less than 255");
            log.warn("The name length must be less than 255");
            return true;
        }

        if (StringUtils.isNotBlank(part.getBrand()) && part.getBrand().length() > 50) {
            response.put("error_brand", "The brand length must be less than 50");
            log.warn("The brand length must be less than 50");
            return true;
        }

        if (part.getWeight() != null && part.getWeight() < 0) {
            response.put("error_weight", "The weight must be greater than 0");
            log.warn("The weight must be greater than 0");
            return true;
        }

        if (part.getMinCount() != null && part.getMinCount() < 1) {
            response.put("error_min_count", "The minimum count must be greater than 0");
            log.warn("The minimum count must be greater than 1");
            return true;
        }

        if (part.getStorageCount() != null && part.getStorageCount() < 0) {
            response.put("error_storage_count", "The storage count must be greater than 0");
            log.warn("The storage count must be greater than 0");
            return true;
        }

        if (part.getReturnPart() != null && part.getReturnPart().compareTo(BigDecimal.ZERO) < 0) {
            response.put("error_return_part", "The return part must be greater than 0");
            log.warn("The return part must be greater than 0");
            return true;
        }

        if (part.getReturnPart() != null && part.getReturnPart().scale() > 2) {
            response.put("error_return_part", "The return part scale must be less than 2");
            log.warn("The return part scale must be less than 2");
            return true;
        }

        if (part.getPrice() != null && part.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            response.put("error_price", "The price must be greater than 0");
            log.warn("The price must be greater than 0");
            return true;
        }

        if (part.getPrice() != null && part.getPrice().scale() > 2) {
            response.put("error_price", "The price scale must be less than 2");
            log.warn("The price scale must be less than 2");
            return true;
        }

        return false;
    }

    /**
     * Validates a uniqueness of new part data.
     *
     * @param part the Part object to validate
     * @param response Map to collect validation error messages
     * @return true if part data is not unique, false otherwise
     */
    private boolean hasNotUniqueData(Part part, Map<String, String> response) {
        if (StringUtils.isNotBlank(part.getArticle())) {
            Optional<Part> partFoundByArticleOptional = iPartRepository.findBySourceIdAndArticleAndBrand(part.getSource().getId(), part.getArticle(), part.getBrand());

            if (partFoundByArticleOptional.isPresent() && !partFoundByArticleOptional.get().getId()
                    .equals(part.getId())) {
                response.put("error_article", "Article and brand already exist in this source");
                log.warn("Article and brand already exist in this source");
                return true;
            }
        }

        return false;
    }

    private boolean canManage() {
        return getCurrentUser().getRole().getPermissions().contains(Permission.PRODUCT_WRITE);
    }

    public Page<PartDto> getSourceParts(UUID sourceId, String text, Pageable pageable) {
        return iPartRepository.findAll(PartQueries.source(sourceId).and(PartQueries.text(text)), pageable)
                .map(this::convertToDto);
    }
}
