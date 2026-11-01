package darkmode.demo.mealplan;

import darkmode.demo.dto.MealPlanEntryDto;
import darkmode.demo.dto.RecipeSummaryDto;
import darkmode.demo.dto.SelectMealRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Päivän ateriasuunnitelma: reseptisivulla hyväksytyt reseptit per
 * ateriatyyppi.
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

    /**
     * Palauttaa valitut ateriat aikaväliltä, esim.
     * GET /api/meal-plan/selections?from=2026-11-01&to=2026-11-30
     * Kalenteri käyttää tätä. params = {"from","to"} erottaa tämän päivän
     * hausta (GET /selections ilman parametreja), joka palauttaa vain tämän
     * päivän valinnat.
     */
    @GetMapping(value = "/selections", params = { "from", "to" })
    public List<MealPlanEntryDto> getSelectionsInRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Authentication authentication) {
        return mealPlanService.getSelections(from, to, authentication);
    }

}
