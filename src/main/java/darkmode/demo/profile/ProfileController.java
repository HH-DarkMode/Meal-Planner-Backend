package darkmode.demo.profile;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import darkmode.demo.auth.User;
import darkmode.demo.auth.UserRepository;
import darkmode.demo.mealplan.WeekPlanService;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.util.Optional;

/**
 * CORS-asetus tulee globaalista CorsConfig-luokasta (app.cors.allowed-origins).
 * Oma @CrossOrigin täällä syrjäyttäisi sen tälle kontrollerille, joten sitä ei käytetä.
 */
@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final WeekPlanService weekPlanService;

    public ProfileController(ProfileRepository profileRepository, UserRepository userRepository,
                             WeekPlanService weekPlanService) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
        this.weekPlanService = weekPlanService;
    }

    @GetMapping
    public Profile getProfile(Authentication authentication) {
        User user = currentUser(authentication);
        return profileRepository.findByUserId(user.getId()).orElseGet(Profile::new);
    }

    /**
     * Tallentaa profiilin. Jos kalorit, makrot, ateriamäärä tai ruokavaliot muuttuvat,
     * tämän päivän ja tulevien päivien ehdotukset poistetaan, ja viikkonäkymä luo uudet.
     * today (valinnainen) tulee selaimesta. Jos sitä ei anneta, käytetään palvelimen päivää.
     */
    @PutMapping
    public Profile saveProfile(@RequestBody Profile profile,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate today,
                               Authentication authentication) {
        User user = currentUser(authentication);
        Optional<Profile> existing = profileRepository.findByUserId(user.getId());
        // Verrataan ENNEN tallennusta, mikä muuttui.
        boolean planChanged = existing.map(old -> WeekPlanService.affectsPlan(old, profile)).orElse(false);
        existing.ifPresent(old -> profile.setId(old.getId()));
        profile.setUser(user);
        Profile saved = profileRepository.save(profile);
        if (planChanged) {
            weekPlanService.resetFutureSuggestions(saved, today != null ? today : LocalDate.now());
        }
        return saved;
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user no longer exists"));
    }
}
