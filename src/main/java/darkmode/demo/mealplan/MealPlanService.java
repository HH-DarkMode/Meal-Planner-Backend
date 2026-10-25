package darkmode.demo.mealplan;

import darkmode.demo.auth.User;
import darkmode.demo.auth.UserRepository;
import darkmode.demo.dto.RecipeSummaryDto;
import darkmode.demo.entity.Recipe;
import darkmode.demo.profile.Profile;
import darkmode.demo.profile.ProfileRepository;
import darkmode.demo.repository.RecipeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tallentaa käyttäjän reseptisivulla hyväksymät reseptit päivän ateriasuunnitelmaan.
 * Valinnat tallennetaan kirjautuneen käyttäjän profiilille.
 */
@Service
public class MealPlanService {

    private static final List<String> MEAL_TYPES = List.of("Breakfast", "Lunch", "Snack", "Dinner");

    private final MealPlanSelectionRepository selectionRepository;
    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;

    public MealPlanService(
            MealPlanSelectionRepository selectionRepository,
            RecipeRepository recipeRepository,
            UserRepository userRepository,
            ProfileRepository profileRepository) {
        this.selectionRepository = selectionRepository;
        this.recipeRepository = recipeRepository;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, RecipeSummaryDto> getTodaysSelections(Authentication authentication) {
        Map<String, RecipeSummaryDto> result = new LinkedHashMap<>();
        Long profileId = currentProfileId(authentication);
        if (profileId == null) return result;
        for (MealPlanSelection selection : selectionRepository.findByProfileIdAndPlanDate(profileId, LocalDate.now())) {
            result.put(selection.getMealType(), toDto(selection.getRecipe()));
        }
        return result;
    }

    @Transactional
    public RecipeSummaryDto selectMeal(String mealType, Integer recipeId, Authentication authentication) {
        if (!MEAL_TYPES.contains(mealType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tuntematon ateriatyyppi: " + mealType);
        }
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reseptia ei löydy"));
        Long profileId = currentProfileId(authentication);
        if (profileId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tallenna profiili ennen aterian valitsemista");
        }

        LocalDate today = LocalDate.now();
        MealPlanSelection selection = selectionRepository
                .findByProfileIdAndPlanDateAndMealType(profileId, today, mealType)
                .orElseGet(MealPlanSelection::new);
        selection.setProfileId(profileId);
        selection.setPlanDate(today);
        selection.setMealType(mealType);
        selection.setRecipe(recipe);
        selectionRepository.save(selection);
        return toDto(recipe);
    }

    @Transactional
    public void clearTodaysSelections(Authentication authentication) {
        Long profileId = currentProfileId(authentication);
        if (profileId != null) {
            selectionRepository.deleteByProfileIdAndPlanDate(profileId, LocalDate.now());
        }
    }

    private Long currentProfileId(Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user no longer exists"));
        return profileRepository.findByUserId(user.getId()).map(Profile::getId).orElse(null);
    }

    private static RecipeSummaryDto toDto(Recipe recipe) {
        return new RecipeSummaryDto(
                recipe.getRecipeId(),
                recipe.getName(),
                recipe.getUrl(),
                recipe.getPrepTimeMin(),
                recipe.getCookTimeMin(),
                recipe.getKcalPerServing(),
                recipe.getProteinPerServing(),
                recipe.getFatPerServing(),
                recipe.getCarbsPerServing(),
                recipe.getNutritionSource(),
                List.of(),
                false
        );
    }
}
