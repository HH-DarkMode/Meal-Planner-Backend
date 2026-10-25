package darkmode.demo.mealplan;

import darkmode.demo.auth.User;
import darkmode.demo.auth.UserRepository;
import darkmode.demo.profile.Profile;
import darkmode.demo.profile.ProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;

class MealPlanServiceTest {

    private MealPlanSelectionRepository selectionRepository;
    private UserRepository userRepository;
    private ProfileRepository profileRepository;
    private MealPlanService service;

    @BeforeEach
    void setUp() {
        selectionRepository = org.mockito.Mockito.mock(MealPlanSelectionRepository.class);
        userRepository = org.mockito.Mockito.mock(UserRepository.class);
        profileRepository = org.mockito.Mockito.mock(ProfileRepository.class);
        service = new MealPlanService(
                selectionRepository,
                org.mockito.Mockito.mock(darkmode.demo.repository.RecipeRepository.class),
                userRepository,
                profileRepository);
    }

    @Test
    void returnsNoSelectionsWhenUserHasNoProfile() {
        User user = new User();
        user.setUsername("user-a");
        org.mockito.Mockito.when(userRepository.findByUsername("user-a")).thenReturn(java.util.Optional.of(user));
        org.mockito.Mockito.when(profileRepository.findByUserId(null)).thenReturn(java.util.Optional.empty());

        var result = service.getTodaysSelections(authentication("user-a"));

        assertThat(result).isEmpty();
        org.mockito.Mockito.verifyNoInteractions(selectionRepository);
    }

    @Test
    void loadsSelectionsUsingAuthenticatedUsersProfileId() {
        User user = new User();
        user.setUsername("user-a");
        Profile profile = new Profile();
        profile.setId(42L);
        org.mockito.Mockito.when(userRepository.findByUsername("user-a")).thenReturn(java.util.Optional.of(user));
        org.mockito.Mockito.when(profileRepository.findByUserId(null)).thenReturn(java.util.Optional.of(profile));
        org.mockito.Mockito.when(selectionRepository.findByProfileIdAndPlanDate(
                org.mockito.Mockito.eq(42L), org.mockito.Mockito.any())).thenReturn(java.util.List.of());

        service.getTodaysSelections(authentication("user-a"));

        org.mockito.Mockito.verify(selectionRepository).findByProfileIdAndPlanDate(
                org.mockito.Mockito.eq(42L), org.mockito.Mockito.any());
    }

    private UsernamePasswordAuthenticationToken authentication(String username) {
        return new UsernamePasswordAuthenticationToken(username, null);
    }
}
