package darkmode.demo.service;

import darkmode.demo.dto.RecipeSearchRequest;
import darkmode.demo.entity.Recipe;
import darkmode.demo.mealplan.MealSlots.MealSlot;
import darkmode.demo.repository.RecipeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Vaihe 3: viikkosuunnitelman reseptihaku (findRecipesForMeal). */
class RecipeSearchServiceTest {

    /** Päivätavoite 2000 kcal, ei makroja eikä ruokavalioita. */
    private static final RecipeSearchRequest DAILY = new RecipeSearchRequest(2000.0, null, null, null, 1, 0.2, Set.of());
    /** Lounas: 30 % päivän energiasta, Lunch-kategoriasta. */
    private static final MealSlot LUNCH = new MealSlot("Lunch", 0.30, "Lunch");

    private RecipeRepository recipeRepository;
    private RecipeSearchService service;

    @BeforeEach
    void setUp() {
        recipeRepository = mock(RecipeRepository.class);
        service = new RecipeSearchService(recipeRepository);
    }

    /** Viikolle tarvitaan eri reseptit, eikä jo käytettyjä (excludeRecipeIds) saa ehdottaa. */
    @Test
    void findRecipesForMealReturnsDistinctRecipesAndSkipsExcluded() {
        // valmistelu: tietokannasta löytyy 10 sopivaa reseptiä (id 1-10)
        when(recipeRepository.searchByCalories(any(), any(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(recipes(10));

        // teko: tarvitaan 5 reseptiä, mutta reseptit 1 ja 2 on jo käytetty viikolla
        List<Recipe> result = service.findRecipesForMeal(DAILY, LUNCH, 5, null, Set.of(1, 2));

        // tarkistus: 5 eri reseptiä, joista mikään ei ole 1 tai 2
        assertThat(result).hasSize(5);
        assertThat(result).extracting(Recipe::getRecipeId)
                .doesNotHaveDuplicates()
                .doesNotContain(1, 2);
    }

    /** Lounas (30 %) ja 2000 kcal -> haetaan 600 kcal ±20 % eli 480–720 kcal Lunch-kategoriasta. */
    @Test
    void findRecipesForMealScalesCaloriesToMealShare() {
        when(recipeRepository.searchByCalories(any(), any(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(List.of());

        service.findRecipesForMeal(DAILY, LUNCH, 7, null, Set.of());

        ArgumentCaptor<Double> min = ArgumentCaptor.forClass(Double.class);
        ArgumentCaptor<Double> max = ArgumentCaptor.forClass(Double.class);
        verify(recipeRepository).searchByCalories(min.capture(), max.capture(), eq("Lunch"), eq(false), eq(false));
        // within: liukulukujen pyöristysvirhe sallitaan
        assertThat(min.getValue()).isCloseTo(480.0, within(0.001));
        assertThat(max.getValue()).isCloseTo(720.0, within(0.001));
    }

    /** Jos sopivia reseptejä on vähemmän kuin päiviä, palautetaan ne mitä on (ei kaadu). */
    @Test
    void findRecipesForMealReturnsFewerWhenNotEnoughMatches() {
        when(recipeRepository.searchByCalories(any(), any(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(recipes(3));

        List<Recipe> result = service.findRecipesForMeal(DAILY, LUNCH, 7, null, Set.of());

        assertThat(result).hasSize(3);
    }

    /** Luo reseptit id:illä 1..count. Kalorit 600 kcal, jotta makrovertailu toimii. */
    private static List<Recipe> recipes(int count) {
        return IntStream.rangeClosed(1, count).mapToObj(id -> {
            Recipe recipe = new Recipe();
            recipe.setRecipeId(id);
            recipe.setName("Recipe " + id);
            recipe.setKcalPerServing(600.0);
            return recipe;
        }).toList();
    }
}
