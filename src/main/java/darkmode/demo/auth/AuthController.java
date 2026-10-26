package darkmode.demo.auth;

import java.time.Duration;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final boolean secureCookie;

    public AuthController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            @org.springframework.beans.factory.annotation.Value("${app.cookie.secure}") boolean secureCookie) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.secureCookie = secureCookie;
    }

    @PostMapping("/register")
    public AuthResponse register(@RequestBody Credentials credentials, jakarta.servlet.http.HttpServletResponse response) {
        validate(credentials);
        if (userRepository.existsByUsername(credentials.username().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is already taken");
        }
        User user = new User();
        user.setUsername(credentials.username().trim());
        user.setPasswordHash(passwordEncoder.encode(credentials.password()));
        userRepository.save(user);
        writeCookie(response, jwtService.createToken(user));
        return new AuthResponse(user.getUsername());
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody Credentials credentials, jakarta.servlet.http.HttpServletResponse response) {
        validate(credentials);
        User user = userRepository.findByUsername(credentials.username().trim())
                .filter(candidate -> passwordEncoder.matches(credentials.password(), candidate.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));
        writeCookie(response, jwtService.createToken(user));
        return new AuthResponse(user.getUsername());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(jakarta.servlet.http.HttpServletResponse response) {
        response.addHeader("Set-Cookie", cookie("", Duration.ZERO).toString());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public AuthResponse me(org.springframework.security.core.Authentication authentication) {
        // /api/auth/** on julkinen, joten kirjautumattomalla pyynnöllä authentication on null.
        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not logged in");
        }
        return new AuthResponse(authentication.getName());
    }

    private void validate(Credentials credentials) {
        if (credentials == null || credentials.username() == null || credentials.username().isBlank()
                || credentials.password() == null || credentials.password().length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username and a password of at least 8 characters are required");
        }
    }

    private void writeCookie(jakarta.servlet.http.HttpServletResponse response, String token) {
        response.addHeader("Set-Cookie", cookie(token, Duration.ofMinutes(15)).toString());
    }

    private ResponseCookie cookie(String value, Duration maxAge) {
        return ResponseCookie.from("access_token", value)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite(secureCookie ? "None" : "Lax")
                .path("/")
                .maxAge(maxAge)
                .build();
    }

    public record Credentials(String username, String password) {}
    public record AuthResponse(String username) {}
}
