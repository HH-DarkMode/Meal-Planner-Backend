package darkmode.demo.mealplan;

import darkmode.demo.dto.MealPlanEntryDto;
import darkmode.demo.dto.RecipeSummaryDto;
import darkmode.demo.dto.WeekPlanDto;
import darkmode.demo.entity.Recipe;
import darkmode.demo.profile.Profile;
import darkmode.demo.service.RecipeSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Vaihe 4: viikkosuunnitelman säännöt (WeekPlanService). */
class WeekPlanServiceTest {

    private static final LocalDate MONDAY = LocalDate.of(2026, 11, 9);
    private static final LocalDate TUESDAY = MONDAY.plusDays(1);
    private static final LocalDate WEDNESDAY = MONDAY.plusDays(2);
    private static final LocalDate SUNDAY = MONDAY.plusDays(6);

    private MealPlanSelectionRepository selectionRepository;
    private RecipeSearchService recipeSearchService;
    private MealPlanService mealPlanService;
    private WeekPlanService service;
    private Profile profile;

    @BeforeEach
    void setUp() {
        selectionRepository = mock(MealPlanSelectionRepository.class);
        recipeSearchService = mock(RecipeSearchService.class);
        mealPlanService = mock(MealPlanService.class);
        service = new WeekPlanService(selectionRepository, recipeSearchService, mealPlanService);

        // Kirjautuneella käyttäjällä on profiili 42: 4 ateriaa, 2000 kcal, ei ruokavalioita.
        profile = new Profile();
        profile.setId(42L);
        profile.setMealsPerDay(4);
        profile.setCalories(2000);
        when(mealPlanService.currentProfile(any())).thenReturn(Optional.of(profile));

        // Tallennus palauttaa saman rivin / listan, kuten oikea tietokanta.
        when(selectionRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(selectionRepository.save(any(MealPlanSelection.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ---------- getWeek ----------

    /** Keskiviikkona ehdotukset luodaan vain ke–su (5 päivää x 4 ateriaa). Ma ja ti jäävät tyhjiksi. */
    @Test
    void getWeekCreatesSuggestionsOnlyForTodayAndFuture() {
        givenWeekRows(List.of());
        givenSearchReturnsUniqueRecipes();

        WeekPlanDto week = service.getWeek(MONDAY, WEDNESDAY, authentication());

        assertThat(week.entries()).hasSize(5 * 4);
        assertThat(week.entries()).allMatch(e -> !e.date().isBefore(WEDNESDAY));
        assertThat(week.entries()).allMatch(e -> e.status().equals("SUGGESTED"));
    }

    /** Olemassa olevaa riviä (hyväksytty ke lounas) ei korvata, eikä sille luoda uutta ehdotusta. */
    @Test
    void getWeekKeepsExistingRows() {
        MealPlanSelection acceptedLunch = row(WEDNESDAY, "Lunch", 99, MealPlanStatus.ACCEPTED);
        givenWeekRows(List.of(acceptedLunch));
        givenSearchReturnsUniqueRecipes();

        WeekPlanDto week = service.getWeek(MONDAY, WEDNESDAY, authentication());

        // edelleen 20 riviä: 19 uutta ehdotusta + 1 vanha hyväksytty lounas
        assertThat(week.entries()).hasSize(20);
        List<MealPlanEntryDto> wednesdayLunches = week.entries().stream()
                .filter(e -> e.date().equals(WEDNESDAY) && e.mealType().equals("Lunch")).toList();
        assertThat(wednesdayLunches).hasSize(1);
        assertThat(wednesdayLunches.get(0).status()).isEqualTo("ACCEPTED");
    }

    /** Ateriat tulevat profiilin ateriamäärästä: 5 ateriaa -> 5 paikkaa oikeassa järjestyksessä. */
    @Test
    void getWeekUsesProfileMealCount() {
        profile.setMealsPerDay(5);
        givenWeekRows(List.of());
        givenSearchReturnsUniqueRecipes();

        WeekPlanDto week = service.getWeek(MONDAY, MONDAY, authentication());

        assertThat(week.mealTypes()).containsExactly("Breakfast", "Lunch", "Snack", "Dinner", "Supper");
        assertThat(week.entries()).hasSize(7 * 5);
    }

    /** Mennyt viikko: uusia ehdotuksia ei luoda eikä reseptejä haeta. */
    @Test
    void getWeekForPastWeekCreatesNothing() {
        givenWeekRows(List.of());

        WeekPlanDto week = service.getWeek(MONDAY, MONDAY.plusDays(7), authentication());

        assertThat(week.entries()).isEmpty();
        verify(recipeSearchService, never()).findRecipesForMeal(any(), any(), anyInt(), any(), any());
    }

    /** Vegaanilla ja tuntemattomilla ainesosilla diet-varoitus näkyy viikkonäkymässä. */
    @Test
    void getWeekShowsDietWarningForUnknownIngredients() {
        profile.setDiets(List.of("vegan"));
        givenWeekRows(List.of(row(MONDAY, "Lunch", 5, MealPlanStatus.ACCEPTED)));
        when(recipeSearchService.toSummary(any(), eq(true))).thenReturn(summary(5, true));

        // mennyt viikko, jotta uusia ehdotuksia ei luoda
        WeekPlanDto week = service.getWeek(MONDAY, MONDAY.plusDays(7), authentication());

        assertThat(week.entries().get(0).recipe().dietWarning()).isTrue();
        verify(recipeSearchService).toSummary(any(), eq(true));
    }

    /** Ilman ainesosista tarkistettavaa ruokavaliota (esim. pelkkä high-protein) varoitusta ei näytetä. */
    @Test
    void getWeekShowsNoDietWarningWithoutIngredientBasedDiet() {
        profile.setDiets(List.of("high-protein"));
        givenWeekRows(List.of(row(MONDAY, "Lunch", 5, MealPlanStatus.ACCEPTED)));

        service.getWeek(MONDAY, MONDAY.plusDays(7), authentication());

        verify(recipeSearchService).toSummary(any(), eq(false));
        verify(recipeSearchService, never()).toSummary(any(), eq(true));
    }

    // ---------- hyväksyntä ----------

    /** Hyväksyntä vaihtaa tilan ACCEPTED, peruminen takaisin SUGGESTED. */
    @Test
    void setAcceptedChangesStatus() {
        MealPlanSelection lunch = givenRow(WEDNESDAY, "Lunch", MealPlanStatus.SUGGESTED);

        service.setAccepted(WEDNESDAY, "Lunch", true, authentication());
        assertThat(lunch.getStatus()).isEqualTo(MealPlanStatus.ACCEPTED);

        service.setAccepted(WEDNESDAY, "Lunch", false, authentication());
        assertThat(lunch.getStatus()).isEqualTo(MealPlanStatus.SUGGESTED);
    }

    /** Syötyä ateriaa ei voi poistaa suunnitelmasta: virhe 409, ja tila pysyy hyväksyttynä. */
    @Test
    void setAcceptedRejectsUnacceptingEatenMeal() {
        MealPlanSelection lunch = givenRow(WEDNESDAY, "Lunch", MealPlanStatus.ACCEPTED);
        lunch.setEaten(true);

        assertThatThrownBy(() -> service.setAccepted(WEDNESDAY, "Lunch", false, authentication()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        assertThat(lunch.getStatus()).isEqualTo(MealPlanStatus.ACCEPTED);
    }

    /** Accept all: päivän kaikki ehdotukset muuttuvat hyväksytyiksi. */
    @Test
    void acceptDayAcceptsAllSuggestions() {
        MealPlanSelection breakfast = row(WEDNESDAY, "Breakfast", 1, MealPlanStatus.SUGGESTED);
        MealPlanSelection dinner = row(WEDNESDAY, "Dinner", 2, MealPlanStatus.SUGGESTED);
        when(selectionRepository.findByProfileIdAndPlanDateAndStatus(42L, WEDNESDAY, MealPlanStatus.SUGGESTED))
                .thenReturn(List.of(breakfast, dinner));

        service.acceptDay(WEDNESDAY, authentication());

        assertThat(breakfast.getStatus()).isEqualTo(MealPlanStatus.ACCEPTED);
        assertThat(dinner.getStatus()).isEqualTo(MealPlanStatus.ACCEPTED);
    }

    // ---------- Vaihda ----------

    /** Hyväksyttyä ateriaa ei voi vaihtaa: virhe 409. */
    @Test
    void rerollRejectsAcceptedMeal() {
        givenRow(WEDNESDAY, "Lunch", MealPlanStatus.ACCEPTED);

        assertThatThrownBy(() -> service.reroll(WEDNESDAY, "Lunch", WEDNESDAY, authentication()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    /** Menneen päivän ateriaa ei voi vaihtaa: virhe 400. */
    @Test
    void rerollRejectsPastDay() {
        assertThatThrownBy(() -> service.reroll(TUESDAY, "Lunch", WEDNESDAY, authentication()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    /** Vaihda antaa uuden reseptin, eikä ehdota viikolla jo olevaa (nykyinen resepti 1 jätetään pois). */
    @Test
    void rerollReplacesRecipeWithNewOne() {
        MealPlanSelection lunch = givenRow(WEDNESDAY, "Lunch", MealPlanStatus.SUGGESTED);
        givenWeekRows(List.of(lunch));
        when(recipeSearchService.findRecipesForMeal(any(), any(), eq(1), any(), any()))
                .thenReturn(List.of(recipe(2)));

        service.reroll(WEDNESDAY, "Lunch", WEDNESDAY, authentication());

        assertThat(lunch.getRecipe().getRecipeId()).isEqualTo(2);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<Integer>> excluded = ArgumentCaptor.forClass(Set.class);
        verify(recipeSearchService).findRecipesForMeal(any(), any(), eq(1), any(), excluded.capture());
        assertThat(excluded.getValue()).contains(1);
    }

    // ---------- profiilimuutos ----------

    /** Profiilimuutos poistaa tulevat ehdotukset ja pois jääneiden aterioiden rivit. */
    @Test
    void resetFutureSuggestionsDeletesSuggestionsAndRemovedMeals() {
        service.resetFutureSuggestions(profile, WEDNESDAY);

        verify(selectionRepository).deleteByProfileIdAndPlanDateGreaterThanEqualAndStatus(
                42L, WEDNESDAY, MealPlanStatus.SUGGESTED);
        verify(selectionRepository).deleteByProfileIdAndPlanDateGreaterThanEqualAndMealTypeNotInAndEatenFalse(
                42L, WEDNESDAY, List.of("Breakfast", "Lunch", "Dinner", "Supper"));
    }

    /** Vain kalorit, makrot, ateriamäärä ja ruokavaliot vaikuttavat ehdotuksiin. Paino ei. */
    @Test
    void affectsPlanDetectsOnlyRelevantChanges() {
        Profile before = profileWith(2000, 4, List.of("vegan", "gluten-free"));

        assertThat(WeekPlanService.affectsPlan(before, profileWith(2200, 4, List.of("vegan", "gluten-free")))).isTrue();
        assertThat(WeekPlanService.affectsPlan(before, profileWith(2000, 6, List.of("vegan", "gluten-free")))).isTrue();
        assertThat(WeekPlanService.affectsPlan(before, profileWith(2000, 4, List.of("vegan")))).isTrue();
        // sama ruokavalio eri järjestyksessä = ei muutosta
        assertThat(WeekPlanService.affectsPlan(before, profileWith(2000, 4, List.of("gluten-free", "vegan")))).isFalse();
        // painon muutos ei vaikuta ehdotuksiin
        Profile heavier = profileWith(2000, 4, List.of("vegan", "gluten-free"));
        heavier.setWeight(80);
        assertThat(WeekPlanService.affectsPlan(before, heavier)).isFalse();
    }

    // ---------- apumetodit ----------

    private void givenWeekRows(List<MealPlanSelection> rows) {
        when(selectionRepository.findByProfileIdAndPlanDateBetweenOrderByPlanDate(42L, MONDAY, SUNDAY))
                .thenReturn(new ArrayList<>(rows));
    }

    /** Reseptihaku palauttaa joka kutsulla pyydetyn määrän uusia reseptejä (id:t eivät toistu). */
    private void givenSearchReturnsUniqueRecipes() {
        AtomicInteger nextId = new AtomicInteger(1000);
        when(recipeSearchService.findRecipesForMeal(any(), any(), anyInt(), any(), any()))
                .thenAnswer(inv -> IntStream.range(0, inv.<Integer>getArgument(2))
                        .mapToObj(i -> recipe(nextId.getAndIncrement()))
                        .toList());
    }

    /** Käyttäjän rivi annetulle päivälle ja aterialle, resepti 1. */
    private MealPlanSelection givenRow(LocalDate date, String mealType, MealPlanStatus status) {
        MealPlanSelection selection = row(date, mealType, 1, status);
        when(selectionRepository.findByProfileIdAndPlanDateAndMealType(42L, date, mealType))
                .thenReturn(Optional.of(selection));
        return selection;
    }

    private static MealPlanSelection row(LocalDate date, String mealType, int recipeId, MealPlanStatus status) {
        MealPlanSelection selection = new MealPlanSelection();
        selection.setProfileId(42L);
        selection.setPlanDate(date);
        selection.setMealType(mealType);
        selection.setRecipe(recipe(recipeId));
        selection.setStatus(status);
        return selection;
    }

    private static Recipe recipe(int id) {
        Recipe recipe = new Recipe();
        recipe.setRecipeId(id);
        recipe.setName("Recipe " + id);
        return recipe;
    }

    private static RecipeSummaryDto summary(int id, boolean dietWarning) {
        return new RecipeSummaryDto(id, "Recipe " + id, null, null, null, 600.0, 30.0, 20.0, 60.0,
                null, dietWarning ? List.of("chicken") : List.of(), dietWarning);
    }

    private static Profile profileWith(int calories, int mealsPerDay, List<String> diets) {
        Profile p = new Profile();
        p.setCalories(calories);
        p.setMealsPerDay(mealsPerDay);
        p.setDiets(diets);
        return p;
    }

    private static UsernamePasswordAuthenticationToken authentication() {
        return new UsernamePasswordAuthenticationToken("user-a", null);
    }
}
