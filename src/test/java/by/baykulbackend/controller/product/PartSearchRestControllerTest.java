package by.baykulbackend.controller.product;

import by.baykulbackend.database.dto.product.PartByArticlesRequestDto;
import by.baykulbackend.database.dto.product.PartByArticlesResponseDto;
import by.baykulbackend.database.dto.product.PartDto;
import by.baykulbackend.services.product.PartService;
import by.baykulbackend.security.JwtFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PartSearchRestController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
public class PartSearchRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PartService partService;

    @MockBean
    private JwtFilter jwtFilter;

    @Test
    public void searchByArticles_ShouldReturnParts() throws Exception {
        // Arrange
        List<String> articles = List.of("2405947", "2405948");
        PartByArticlesRequestDto request = PartByArticlesRequestDto.builder()
                .articles(articles)
                .build();

        PartDto part1 = PartDto.builder()
                .article("2405947")
                .name("Engine Oil")
                .brand("BMW")
                .build();

        PartByArticlesResponseDto response = PartByArticlesResponseDto.builder()
                .parts(List.of(part1))
                .build();

        when(partService.getPartsByArticles(any(PartByArticlesRequestDto.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/v1/product/search/articles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.parts").isArray())
                .andExpect(jsonPath("$.parts[0].article").value("2405947"))
                .andExpect(jsonPath("$.parts[0].name").value("Engine Oil"));

        verify(partService).getPartsByArticles(any(PartByArticlesRequestDto.class));
    }
}
