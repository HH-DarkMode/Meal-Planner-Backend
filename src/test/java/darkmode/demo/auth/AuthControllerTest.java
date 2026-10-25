package darkmode.demo.auth;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthControllerTest {

    private UserRepository userRepository;
    private AuthController controller;

    @BeforeEach
    void setUp() {
        userRepository = org.mockito.Mockito.mock(UserRepository.class);
        controller = new AuthController(
                userRepository,
                new BCryptPasswordEncoder(),
                new JwtService("test-secret-with-at-least-32-characters", 900_000),
                false);
    }

    @Test
    void rejectsMissingCredentials() {
        assertThatThrownBy(() -> controller.login(new AuthController.Credentials(" ", "short"), response()))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("at least 8 characters");
    }

    @Test
    void rejectsDuplicateRegistration() {
        org.mockito.Mockito.when(userRepository.existsByUsername("existing")).thenReturn(true);

        assertThatThrownBy(() -> controller.register(
                new AuthController.Credentials("existing", "password-123"), response()))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("already taken");
    }

    @Test
    void storesPasswordAsAHashWhenRegistering() {
        org.mockito.Mockito.when(userRepository.existsByUsername("new-user")).thenReturn(false);
        org.mockito.Mockito.when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        HttpServletResponse response = response();

        AuthController.AuthResponse result = controller.register(
                new AuthController.Credentials(" new-user ", "password-123"), response);

        assertThat(result.username()).isEqualTo("new-user");
        org.mockito.ArgumentCaptor<User> captor = org.mockito.ArgumentCaptor.forClass(User.class);
        org.mockito.Mockito.verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isNotEqualTo("password-123");
        assertThat(new BCryptPasswordEncoder().matches("password-123", captor.getValue().getPasswordHash())).isTrue();
    }

    private HttpServletResponse response() {
        return org.mockito.Mockito.mock(HttpServletResponse.class);
    }
}
