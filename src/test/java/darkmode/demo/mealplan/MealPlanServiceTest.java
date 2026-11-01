package darkmode.demo.mealplan;

import darkmode.demo.auth.User;
import darkmode.demo.auth.UserRepository;
import darkmode.demo.entity.Recipe;
import darkmode.demo.profile.Profile;
import darkmode.demo.profile.ProfileRepository;
import darkmode.demo.repository.RecipeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MealPlanServiceTest {

    private MealPlanSelectionRepository selectionRepository;
    private RecipeRepository recipeRepository;
    private UserRepository userRepository;
    private ProfileRepository profileRepository;
    private MealPlanService service;

    @BeforeEach
    void setUp() {
        selectionRepository = org.mockito.Mockito.mock(MealPlanSelectionRepository.class);
        recipeRepository = org.mockito.Mockito.mock(RecipeRepository.class);
        userRepository = org.mockito.Mockito.mock(UserRepository.class);
        profileRepository = org.mockito.Mockito.mock(ProfileRepository.class);
        service = new MealPlanService(
                selectionRepository,
                recipeRepository,
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

    /** Liian pitkä aikaväli (yli 62 päivää) hylätään virheellä 400. */
    @Test
    void getSelectionsRejectsRangeLongerThan62Days() {
        assertThatThrownBy(() -> service.getSelections(
                LocalDate.of(2026, 11, 1), LocalDate.of(2027, 1, 10), authentication("user-a")))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    /** Loppupäivä ennen alkupäivää hylätään virheellä 400. */
    @Test
    void getSelectionsRejectsEndDateBeforeStartDate() {
        assertThatThrownBy(() -> service.getSelections(
                LocalDate.of(2026, 11, 30), LocalDate.of(2026, 11, 1), authentication("user-a")))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    /** Kalenterin haku: haetaan kirjautuneen käyttäjän profiilin id:llä ja palautetaan päivä, tila ja makrot. */
    @Test
    void getSelectionsReturnsEntriesOnlyForAuthenticatedUsersProfile() {
        // valmistelu: käyttäjä, jolla on profiili 42, ja yksi tallennettu ateria
        givenUserWithProfile(42L);
        MealPlanSelection selection = new MealPlanSelection();
        selection.setPlanDate(LocalDate.of(2026, 11, 23));
        selection.setMealType("Lunch");
        selection.setEaten(true);
        selection.setRecipe(recipe(5));
        LocalDate from = LocalDate.of(2026, 11, 1);
        LocalDate to = LocalDate.of(2026, 11, 30);
        when(selectionRepository.findByProfileIdAndPlanDateBetweenOrderByPlanDate(42L, from, to))
                .thenReturn(List.of(selection));

        // teko
        var result = service.getSelections(from, to, authentication("user-a"));

        // tarkistus: rivi on tullut oikeilla tiedoilla
        assertThat(result).hasSize(1);
        assertThat(result.get(0).date()).isEqualTo(LocalDate.of(2026, 11, 23));
        assertThat(result.get(0).mealType()).isEqualTo("Lunch");
        assertThat(result.get(0).eaten()).isTrue();
        assertThat(result.get(0).recipe().recipeId()).isEqualTo(5);
    }

    /** Täppä: aterian syöty-tila tallentuu käyttäjän omaan riviin. */
    @Test
    void setEatenUpdatesPlannedMealOfOwnProfile() {
        // valmistelu: käyttäjän profiililla on suunniteltu lounas
        givenUserWithProfile(42L);
        LocalDate date = LocalDate.of(2026, 11, 23);
        MealPlanSelection selection = new MealPlanSelection();
        when(selectionRepository.findByProfileIdAndPlanDateAndMealType(42L, date, "Lunch"))
                .thenReturn(Optional.of(selection));

        // teko
        service.setEaten(date, "Lunch", true, authentication("user-a"));

        // tarkistus: tila vaihtui ja muutos tallennettiin
        assertThat(selection.isEaten()).isTrue();
        verify(selectionRepository).save(selection);
    }

    /** Suunnittelematonta ateriaa ei voi merkitä syödyksi: virhe 404. */
    @Test
    void setEatenReturns404WhenMealIsNotPlanned() {
        givenUserWithProfile(42L);
        when(selectionRepository.findByProfileIdAndPlanDateAndMealType(
                org.mockito.Mockito.eq(42L), org.mockito.Mockito.any(), org.mockito.Mockito.eq("Lunch")))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.setEaten(LocalDate.of(2026, 11, 23), "Lunch", true, authentication("user-a")))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    /** Resepti voidaan valita myös muulle kuin tälle päivälle (viikkosuunnitelma). */
    @Test
    void selectMealSavesToGivenDate() {
        givenUserWithProfile(42L);
        when(recipeRepository.findById(5)).thenReturn(Optional.of(recipe(5)));
        LocalDate date = LocalDate.of(2026, 11, 25);
        when(selectionRepository.findByProfileIdAndPlanDateAndMealType(42L, date, "Dinner"))
                .thenReturn(Optional.empty());

        service.selectMeal("Dinner", 5, date, authentication("user-a"));

        // tarkistus: tallennetulla rivillä on annettu päivä, eikä sitä ole merkitty syödyksi
        ArgumentCaptor<MealPlanSelection> saved = ArgumentCaptor.forClass(MealPlanSelection.class);
        verify(selectionRepository).save(saved.capture());
        assertThat(saved.getValue().getPlanDate()).isEqualTo(date);
        assertThat(saved.getValue().isEaten()).isFalse();
    }

    /** Vanha toiminta säilyy: ilman päivää resepti tallentuu tälle päivälle. */
    @Test
    void selectMealWithoutDateUsesToday() {
        givenUserWithProfile(42L);
        when(recipeRepository.findById(5)).thenReturn(Optional.of(recipe(5)));
        when(selectionRepository.findByProfileIdAndPlanDateAndMealType(
                org.mockito.Mockito.eq(42L), org.mockito.Mockito.any(), org.mockito.Mockito.eq("Dinner")))
                .thenReturn(Optional.empty());

        service.selectMeal("Dinner", 5, null, authentication("user-a"));

        ArgumentCaptor<MealPlanSelection> saved = ArgumentCaptor.forClass(MealPlanSelection.class);
        verify(selectionRepository).save(saved.capture());
        assertThat(saved.getValue().getPlanDate()).isEqualTo(LocalDate.now());
    }

    /** Kun päivän ateriaksi valitaan toinen resepti, syöty-merkintä nollautuu (uutta ei ole syöty). */
    @Test
    void changingRecipeResetsEatenFlag() {
        // valmistelu: päivällä on jo syöty lounas, resepti 1
        givenUserWithProfile(42L);
        when(recipeRepository.findById(2)).thenReturn(Optional.of(recipe(2)));
        LocalDate date = LocalDate.of(2026, 11, 23);
        MealPlanSelection existing = new MealPlanSelection();
        existing.setRecipe(recipe(1));
        existing.setEaten(true);
        when(selectionRepository.findByProfileIdAndPlanDateAndMealType(42L, date, "Lunch"))
                .thenReturn(Optional.of(existing));

        // teko: lounaaksi valitaan resepti 2
        service.selectMeal("Lunch", 2, date, authentication("user-a"));

        // tarkistus
        assertThat(existing.isEaten()).isFalse();
    }

    /** Luo käyttäjän "user-a", jolla on profiili annetulla id:llä. */
    private void givenUserWithProfile(long profileId) {
        User user = new User();
        user.setUsername("user-a");
        Profile profile = new Profile();
        profile.setId(profileId);
        when(userRepository.findByUsername("user-a")).thenReturn(Optional.of(user));
        when(profileRepository.findByUserId(null)).thenReturn(Optional.of(profile));
    }

    private Recipe recipe(int id) {
        Recipe recipe = new Recipe();
        recipe.setRecipeId(id);
        recipe.setName("Recipe " + id);
        return recipe;
    }

    private UsernamePasswordAuthenticationToken authentication(String username) {
        return new UsernamePasswordAuthenticationToken(username, null);
    }
}
