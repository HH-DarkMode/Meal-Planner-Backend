package darkmode.demo.mealplan;

import darkmode.demo.dto.RecipeSummaryDto;
import darkmode.demo.entity.Recipe;
import darkmode.demo.repository.RecipeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tallentaa käyttäjän reseptisivulla hyväksymät reseptit päivän ateriasuunnitelmaan.
 * Sovelluksessa on toistaiseksi vain yksi profiili (id = 1), ks. profile.ProfileController.
 */
@Service
public class MealPlanService {

    private static final Long PROFILE_ID = 1L;
    static final List<String> MEAL_TYPES = List.of("Breakfast", "Lunch", "Snack", "Dinner");

    private final MealPlanSelectionRepository selectionRepository;
    private final RecipeRepository recipeRepository;

    public MealPlanService(MealPlanSelectionRepository selectionRepository, RecipeRepository recipeRepository) {
        this.selectionRepository = selectionRepository;
        this.recipeRepository = recipeRepository;
    }

    /** Tämän päivän hyväksytyt reseptit ateriatyypeittäin. Puuttuva avain = ei vielä hyväksytty. */
    @Transactional(readOnly = true)
    public Map<String, RecipeSummaryDto> getTodaysSelections() {
        Map<String, RecipeSummaryDto> result = new LinkedHashMap<>();
        for (MealPlanSelection selection : selectionRepository.findByProfileIdAndPlanDate(PROFILE_ID, LocalDate.now())) {
            result.put(selection.getMealType(), toDto(selection.getRecipe()));
        }
        return result;
    }

    /** Hyväksyy reseptin ateriatyypille tälle päivälle. Korvaa aiemman hyväksynnän samalle paikalle. */
    @Transactional
    public RecipeSummaryDto selectMeal(String mealType, Integer recipeId) {
        if (!MEAL_TYPES.contains(mealType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tuntematon ateriatyyppi: " + mealType);
        }
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reseptia ei löydy"));

        LocalDate today = LocalDate.now();
        MealPlanSelection selection = selectionRepository
                .findByProfileIdAndPlanDateAndMealType(PROFILE_ID, today, mealType)
                .orElseGet(MealPlanSelection::new);
        selection.setProfileId(PROFILE_ID);
        selection.setPlanDate(today);
        selection.setMealType(mealType);
        selection.setRecipe(recipe);
        selectionRepository.save(selection);

        return toDto(recipe);
    }

    // dietWarning/unmatchedIngredients eivät ole merkityksellisiä tässä (ei ruokavaliosuodatinta
    // aktiivisena), joten ne jätetään tyhjäksi/falseksi.
    private static RecipeSummaryDto toDto(Recipe r) {
        return new RecipeSummaryDto(
                r.getRecipeId(),
                r.getName(),
                r.getUrl(),
                r.getPrepTimeMin(),
                r.getCookTimeMin(),
                r.getKcalPerServing(),
                r.getProteinPerServing(),
                r.getFatPerServing(),
                r.getCarbsPerServing(),
                r.getNutritionSource(),
                List.of(),
                false
        );
    }
}
