package darkmode.demo.profile;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * CORS-asetus tulee globaalista CorsConfig-luokasta (app.cors.allowed-origins).
 * Oma @CrossOrigin täällä syrjäyttäisi sen tälle kontrollerille, joten sitä ei käytetä.
 */
@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    private final ProfileRepository profileRepository;

    public ProfileController(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    @GetMapping
    public Profile getProfile() {
        return profileRepository.findById(1L)
                .orElseGet(() -> profileRepository.save(defaultProfile()));
    }

    @PutMapping
    public Profile saveProfile(@RequestBody Profile profile) {
        profile.setId(1L);
        return profileRepository.save(profile);
    }

    private Profile defaultProfile() {
        Profile profile = new Profile();

        profile.setUsername("Samuel");
        profile.setWeight(80);
        profile.setHeight(180);
        profile.setGender("mies");
        profile.setGoal("healthy");
        profile.setDiets(List.of("vegetarian"));
        profile.setCalories(2500);
        profile.setProtein(160);
        profile.setFat(70);
        profile.setCarbs(280);

        return profile;
    }
}