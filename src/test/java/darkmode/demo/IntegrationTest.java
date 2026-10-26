package darkmode.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import darkmode.demo.auth.UserRepository;
import darkmode.demo.profile.Profile;
import darkmode.demo.profile.ProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProfileRepository profileRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void cleanDatabase() {
        profileRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void registrationCookieAuthenticatesSubsequentProfileRequests() throws Exception {
        MvcResult registration = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"integration-user","password":"password-123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("integration-user"))
                .andReturn();

        var accessToken = registration.getResponse().getCookie("access_token");
        org.assertj.core.api.Assertions.assertThat(accessToken).isNotNull();

        mockMvc.perform(get("/api/profile").cookie(accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").doesNotExist());

        Profile profile = new Profile();
        profile.setUsername("integration-user");
        profile.setWeight(75);
        profile.setHeight(180);
        profile.setMealsPerDay(4);
        profile.setCalories(2200);
        profile.setProtein(140);
        profile.setFat(70);
        profile.setCarbs(230);
        profile.setDiets(List.of("vegetarian"));

        mockMvc.perform(put("/api/profile")
                        .cookie(accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(profile)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.calories").value(2200))
                .andExpect(jsonPath("$.diets[0]").value("vegetarian"));

        mockMvc.perform(get("/api/profile").cookie(accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("integration-user"))
                .andExpect(jsonPath("$.mealsPerDay").value(4))
                .andExpect(jsonPath("$.calories").value(2200));
    }

    @Test
    void protectedProfileEndpointRejectsRequestsWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meEndpointRejectsRequestsWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publicEndpointsWorkWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }
}
