package darkmode.demo.mealplan;

import darkmode.demo.dto.AcceptDayRequest;
import darkmode.demo.dto.MealPlanEntryDto;
import darkmode.demo.dto.RecipeSummaryDto;
import darkmode.demo.dto.RerollRequest;
import darkmode.demo.dto.SelectMealRequest;
import darkmode.demo.dto.SetAcceptedRequest;
import darkmode.demo.dto.SetEatenRequest;
import darkmode.demo.dto.WeekPlanDto;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
    private final WeekPlanService weekPlanService;

    public MealPlanController(MealPlanService mealPlanService, WeekPlanService weekPlanService) {
        this.mealPlanService = mealPlanService;
        this.weekPlanService = weekPlanService;
    }

    @GetMapping("/selections")
    public Map<String, RecipeSummaryDto> getSelections(Authentication authentication) {
        return mealPlanService.getTodaysSelections(authentication);
    }

    @PutMapping("/selections")
    public RecipeSummaryDto selectMeal(@RequestBody SelectMealRequest request, Authentication authentication) {
        return mealPlanService.selectMeal(request.mealType(), request.recipeId(), request.date(), authentication);
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

    /**
     * Merkitsee aterian syödyksi tai syömättömäksi (mealplan-sivun täppä).
     * Pyynnön runko: { "date": "2026-11-23", "mealType": "Lunch", "eaten": true }
     */
    @PatchMapping("/selections/eaten")
    public void setEaten(@RequestBody SetEatenRequest request, Authentication authentication) {
        mealPlanService.setEaten(request.date(), request.mealType(), request.eaten(), authentication);
    }

    /**
     * Viikkonäkymä. Esim. GET /api/meal-plan/week?start=2026-11-09&today=2026-11-09
     * Luo puuttuvat ehdotukset ja palauttaa viikon.
     *
     * Uusintayritys: Reactin StrictMode ajaa kehitystilassa haun kahdesti samaan aikaan. Silloin
     * molemmat voivat yrittää luoda saman ehdotuksen, ja tietokannan UNIQUE-ehto hylkää toisen.
     * Toinen yritys löytää jo luodut rivit eikä yritä luoda niitä uudelleen.
     */
    @GetMapping("/week")
    public WeekPlanDto getWeek(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate today,
            Authentication authentication) {
        try {
            return weekPlanService.getWeek(start, today, authentication);
        } catch (DataIntegrityViolationException e) {
            return weekPlanService.getWeek(start, today, authentication);
        }
    }

    /** Hyväksy tai peru: PATCH /api/meal-plan/selections/status { date, mealType, accepted } */
    @PatchMapping("/selections/status")
    public void setAccepted(@RequestBody SetAcceptedRequest request, Authentication authentication) {
        weekPlanService.setAccepted(request.date(), request.mealType(), request.accepted(), authentication);
    }

    /** Vaihda: POST /api/meal-plan/selections/reroll { date, mealType, today }. Palauttaa uuden rivin. */
    @PostMapping("/selections/reroll")
    public MealPlanEntryDto reroll(@RequestBody RerollRequest request, Authentication authentication) {
        return weekPlanService.reroll(request.date(), request.mealType(), request.today(), authentication);
    }

    /** Accept all: POST /api/meal-plan/day/accept { date } */
    @PostMapping("/day/accept")
    public void acceptDay(@RequestBody AcceptDayRequest request, Authentication authentication) {
        weekPlanService.acceptDay(request.date(), authentication);
    }

    /** Käyttäjän ateriat järjestyksessä (kalenteri, vaihe 7): GET /api/meal-plan/meal-types */
    @GetMapping("/meal-types")
    public List<String> mealTypes(Authentication authentication) {
        return weekPlanService.mealTypes(authentication);
    }

}
