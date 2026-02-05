package by.baykulbackend.services.product;

import by.baykulbackend.database.dao.product.Currency;
import by.baykulbackend.database.dao.product.Part;
import by.baykulbackend.database.repository.product.IPartRepository;
import by.baykulbackend.exceptions.NotFoundException;
import by.baykulbackend.services.user.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PartService {
    private final IPartRepository iPartRepository;
    private final AuthService authService;

    /**
     * Creates a new part in the system.
     *
     * @param part the Part object to create
     * @return ResponseEntity with success/error message
     */
    public ResponseEntity<?> createPart(Part part) {
        Map<String, String> response = new HashMap<>();

        if (isNotValidNewPart(part, response)) {
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

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
     * Updates an existing part's information.
     * Only updates non-null fields from the provided part object.
     *
     * @param id   the UUID of the part to update
     * @param part the Part object containing updated fields
     * @return ResponseEntity with success/error message
     * @throws NotFoundException if no part is found with the given ID
     */
    @SuppressWarnings("BigDecimalMethodWithoutRoundingCalled")
    public ResponseEntity<?> updatePart(UUID id, Part part) {
        Map<String, String> response = new HashMap<>();
        Part partFromDB = iPartRepository.findById(id).orElseThrow(() -> new NotFoundException("Part not found"));

        if (isNotValidPart(part, response)) {
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        if (hasNotUniqueData(part, response)) {
            return new ResponseEntity<>(response, HttpStatus.CONFLICT);
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
    public ResponseEntity<?> deletePartById(UUID id) {
        Map<String, String> response = new HashMap<>();
        Part partFromDB = iPartRepository.findById(id).orElseThrow(() -> new NotFoundException("Part not found"));

        iPartRepository.deleteById(id);
        response.put("delete_part", "true");
        log.info("Delete part with id = {} -> {}", id, authService.getAuthInfo().getPrincipal());

        return ResponseEntity.ok(response);
    }

    /**
     * Searches for parts by article, name containing the given text.
     * Performs case-insensitive search.
     * Returns distinct parts to avoid duplicates.
     *
     * @param text the search text to match against part attributes
     * @return List of matching Part objects
     */
    public List<Part> searchPart(String text) {
        List<Part> result = new ArrayList<>();
        result.addAll(iPartRepository.findByArticleContainingIgnoreCase(text));
        result.addAll(iPartRepository.findByNameContainingIgnoreCase(text));

        return result.stream().distinct().collect(Collectors.toList());
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

        if (part.getMinCount() < 1) {
            response.put("error_min_count", "The minimum count must be greater than 0");
            log.warn("The minimum count must be greater than 1");
            return true;
        }

        if (part.getStorageCount() != null && part.getStorageCount() < 0) {
            response.put("error_storage_count", "The storage count must be greater than 0");
            log.warn("The storage count must be greater than 0");
            return true;
        }

        if (part.getReturnPart().compareTo(BigDecimal.ZERO) < 0) {
            response.put("error_return_part", "The return part must be greater than 0");
            log.warn("The return part must be greater than 0");
            return true;
        }

        if (part.getReturnPart().scale() > 2) {
            response.put("error_return_part", "The return part scale must be less than 2");
            log.warn("The return part scale must be less than 2");
            return true;
        }

        if (part.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            response.put("error_price", "The price must be greater than 0");
            log.warn("The price must be greater than 0");
            return true;
        }

        if (part.getPrice().scale() > 2) {
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
            Optional<Part> partFoundByArticleOptional = iPartRepository.findByArticle(part.getArticle());

            if (partFoundByArticleOptional.isPresent() && !partFoundByArticleOptional.get().getId()
                    .equals(part.getId())) {
                response.put("error_article", "Part with that article already exists");
                log.warn("Part with that article already exists");
                return true;
            }
        }

        return false;
    }
}
