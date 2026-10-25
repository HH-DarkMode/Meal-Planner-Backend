package darkmode.demo.mealplan;

import darkmode.demo.dto.RecipeSummaryDto;
import darkmode.demo.dto.SelectMealRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

import java.util.Map;

/**
 * Päivän ateriasuunnitelma: reseptisivulla hyväksytyt reseptit per ateriatyyppi.
 * CORS tulee globaalista CorsConfig-luokasta (sama kuin RecipeController).
 */
@RestController
@RequestMapping("/api/meal-plan")
public class MealPlanController {

    private final MealPlanService mealPlanService;

    public MealPlanController(MealPlanService mealPlanService) {
        this.mealPlanService = mealPlanService;
    }

    @GetMapping("/selections")
    public Map<String, RecipeSummaryDto> getSelections(Authentication authentication) {
        return mealPlanService.getTodaysSelections(authentication);
    }

    @PutMapping("/selections")
    public RecipeSummaryDto selectMeal(@RequestBody SelectMealRequest request, Authentication authentication) {
        return mealPlanService.selectMeal(request.mealType(), request.recipeId(), authentication);
    }

    @DeleteMapping("/selections")
    public void clearSelections(Authentication authentication) {
        mealPlanService.clearTodaysSelections(authentication);
    }
}
