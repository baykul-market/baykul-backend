package by.baykulbackend.services.product;

import by.baykulbackend.database.dao.finance.Currency;
import by.baykulbackend.database.dao.product.Part;
import by.baykulbackend.database.dao.user.User;
import by.baykulbackend.database.dto.product.PartByArticlesRequestDto;
import by.baykulbackend.database.dto.product.PartByArticlesResponseDto;
import by.baykulbackend.database.dto.product.PartDto;
import by.baykulbackend.database.model.Permission;
import by.baykulbackend.database.model.Role;
import by.baykulbackend.database.repository.product.IPartRepository;
import by.baykulbackend.database.repository.user.IUserRepository;
import by.baykulbackend.security.JwtAuthentication;
import by.baykulbackend.services.finance.PriceService;
import by.baykulbackend.services.user.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PartServiceTest {

    @Mock
    private IPartRepository iPartRepository;

    @Mock
    private IUserRepository iUserRepository;

    @Mock
    private AuthService authService;

    @Mock
    private PriceService priceService;

    @InjectMocks
    private PartService partService;

    private User testUser;
    private JwtAuthentication jwtAuthentication;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setLogin("testUser");
        testUser.setRole(Role.USER);

        jwtAuthentication = mock(JwtAuthentication.class);
        lenient().when(jwtAuthentication.getPrincipal()).thenReturn("testUser");
        lenient().when(authService.getAuthInfo()).thenReturn(jwtAuthentication);
        lenient().when(iUserRepository.findByLogin("testUser")).thenReturn(Optional.of(testUser));
        lenient().when(priceService.getSystemCurrency()).thenReturn(Currency.EUR);
    }

    @Test
    void getPartsByArticles_ShouldPreserveOrder() {
        // Arrange
        String artA = "ARTICLE_A";
        String artB = "ARTICLE_B";
        List<String> requestedArticles = List.of(artB, artA);

        Part partA = createPart(artA, "Name A");
        Part partB = createPart(artB, "Name B");

        when(iPartRepository.findAllByArticleIn(anySet())).thenReturn(Set.of(partA, partB));
        when(priceService.calculateProductPrice(any(), anyBoolean(), any())).thenReturn(BigDecimal.TEN);

        PartByArticlesRequestDto request = PartByArticlesRequestDto.builder()
                .articles(requestedArticles)
                .build();

        // Act
        PartByArticlesResponseDto response = partService.getPartsByArticles(request);

        // Assert
        assertNotNull(response);
        assertEquals(2, response.getParts().size());
        assertEquals(artB, response.getParts().get(0).getArticle());
        assertEquals(artA, response.getParts().get(1).getArticle());
        
        // Verify optimization - user should be fetched once
        verify(iUserRepository, times(1)).findByLogin("testUser");
    }

    @Test
    void getPartsByArticles_ShouldHandleDuplicates() {
        // Arrange
        String artA = "ARTICLE_A";
        List<String> requestedArticles = List.of(artA, artA);

        Part partA = createPart(artA, "Name A");

        when(iPartRepository.findAllByArticleIn(anySet())).thenReturn(Set.of(partA));
        when(priceService.calculateProductPrice(any(), anyBoolean(), any())).thenReturn(BigDecimal.TEN);

        PartByArticlesRequestDto request = PartByArticlesRequestDto.builder()
                .articles(requestedArticles)
                .build();

        // Act
        PartByArticlesResponseDto response = partService.getPartsByArticles(request);

        // Assert
        assertNotNull(response);
        assertEquals(2, response.getParts().size());
        assertEquals(artA, response.getParts().get(0).getArticle());
        assertEquals(artA, response.getParts().get(1).getArticle());
    }

    @Test
    void getPartsByArticles_ShouldFilterNotFound() {
        // Arrange
        String artA = "ARTICLE_A";
        String artC = "ARTICLE_C"; // Not in DB
        List<String> requestedArticles = List.of(artA, artC);

        Part partA = createPart(artA, "Name A");

        when(iPartRepository.findAllByArticleIn(anySet())).thenReturn(Set.of(partA));
        when(priceService.calculateProductPrice(any(), anyBoolean(), any())).thenReturn(BigDecimal.TEN);

        PartByArticlesRequestDto request = PartByArticlesRequestDto.builder()
                .articles(requestedArticles)
                .build();

        // Act
        PartByArticlesResponseDto response = partService.getPartsByArticles(request);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getParts().size());
        assertEquals(artA, response.getParts().get(0).getArticle());
    }

    @Test
    void getPartsByArticles_ShouldReturnEmptyForNullOrEmptyRequest() {
        // Act & Assert
        assertNotNull(partService.getPartsByArticles(null).getParts());
        assertTrue(partService.getPartsByArticles(null).getParts().isEmpty());

        PartByArticlesRequestDto emptyRequest = PartByArticlesRequestDto.builder()
                .articles(Collections.emptyList())
                .build();
        assertTrue(partService.getPartsByArticles(emptyRequest).getParts().isEmpty());
    }

    private Part createPart(String article, String name) {
        Part part = new Part();
        part.setId(UUID.randomUUID());
        part.setArticle(article);
        part.setName(name);
        part.setBrand("Brand");
        part.setPrice(BigDecimal.TEN);
        part.setCurrency(Currency.EUR);
        return part;
    }
}
