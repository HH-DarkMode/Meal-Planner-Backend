package darkmode.demo.auth;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "test-secret-with-at-least-32-characters";

    @Test
    void createsTokenThatContainsTheUserName() {
        JwtService service = new JwtService(SECRET, 900_000);
        User user = new User();
        user.setUsername("test-user");

        String token = service.createToken(user);

        assertThat(token).isNotBlank();
        assertThat(service.getUsername(token)).isEqualTo("test-user");
    }

    @Test
    void rejectsTokenSignedWithAnotherSecret() {
        JwtService service = new JwtService(SECRET, 900_000);
        JwtService otherService = new JwtService("another-secret-with-at-least-32-chars", 900_000);
        User user = new User();
        user.setUsername("test-user");

        String token = service.createToken(user);

        assertThatThrownBy(() -> otherService.getUsername(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsShortSecrets() {
        assertThatThrownBy(() -> new JwtService("too-short", 900_000))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
