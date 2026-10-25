package darkmode.demo.profile;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import darkmode.demo.auth.User;
import darkmode.demo.auth.UserRepository;
import org.springframework.security.core.Authentication;

/**
 * CORS-asetus tulee globaalista CorsConfig-luokasta (app.cors.allowed-origins).
 * Oma @CrossOrigin täällä syrjäyttäisi sen tälle kontrollerille, joten sitä ei käytetä.
 */
@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;

    public ProfileController(ProfileRepository profileRepository, UserRepository userRepository) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public Profile getProfile(Authentication authentication) {
        User user = currentUser(authentication);
        return profileRepository.findByUserId(user.getId()).orElseGet(Profile::new);
    }

    @PutMapping
    public Profile saveProfile(@RequestBody Profile profile, Authentication authentication) {
        User user = currentUser(authentication);
        profileRepository.findByUserId(user.getId()).ifPresent(existing -> profile.setId(existing.getId()));
        profile.setUser(user);
        return profileRepository.save(profile);
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user no longer exists"));
    }
}