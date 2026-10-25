package darkmode.demo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import darkmode.demo.auth.AuthController;
import darkmode.demo.auth.JwtService;
import darkmode.demo.auth.UserRepository;
import darkmode.demo.dto.RecipeSearchRequest;
import darkmode.demo.dto.RecipeSummaryDto;
import darkmode.demo.service.RecipeDetailService;
import darkmode.demo.service.RecipeSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RestApiTest {

    private MockMvc mockMvc;
    private RecipeSearchService recipeSearchService;

    @BeforeEach
    void setUp() {
        recipeSearchService = mock(RecipeSearchService.class);
        RecipeController recipeController = new RecipeController(
                recipeSearchService,
                mock(RecipeDetailService.class));
        AuthController authController = new AuthController(
                mock(UserRepository.class),
                new BCryptPasswordEncoder(),
                new JwtService("test-secret-with-at-least-32-characters", 900_000),
                false);

        mockMvc = MockMvcBuilders.standaloneSetup(recipeController, authController)
                .setControllerAdvice()
                .build();
    }

    @Test
    void searchEndpointReturnsRecipeJsonAndPassesQueryParametersToService() throws Exception {
        when(recipeSearchService.search(any(RecipeSearchRequest.class), eq(2)))
                .thenReturn(List.of(new RecipeSummaryDto(
                        7, "Tomato pasta", "/recipes/7", 10, 20,
                        450.0, 18.0, 12.0, 62.0, "source", List.of(), false)));

        mockMvc.perform(get("/api/recipes/search")
                        .param("calories", "600")
                        .param("protein", "80")
                        .param("mealsPerDay", "4")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].recipeId").value(7))
                .andExpect(jsonPath("$[0].name").value("Tomato pasta"))
                .andExpect(jsonPath("$[0].caloriesPerServing").value(450.0));
    }

    @Test
    void searchEndpointRejectsSizeOutsideApiContract() throws Exception {
        mockMvc.perform(get("/api/recipes/search").param("size", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerEndpointReturnsUserAndSetsHttpOnlyCookie() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(
                                new AuthController.Credentials("new-user", "password-123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("new-user"))
                .andExpect(cookie().httpOnly("access_token", true));
    }
}
