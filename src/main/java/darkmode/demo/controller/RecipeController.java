package darkmode.demo.controller;

import darkmode.demo.dto.DayPlanDto;
import darkmode.demo.dto.RecipeDetailDto;
import darkmode.demo.dto.RecipeSearchRequest;
import darkmode.demo.dto.RecipeSummaryDto;
import darkmode.demo.service.RecipeDetailService;
import darkmode.demo.service.RecipeSearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

/**
 * Reseptirajapinta (REST). Kaikki endpointit palauttavat JSONia.
 *  - GET /api/recipes/search : ehdotuksia yhdelle ateriale
 *  - GET /api/recipes/plan   : päivän ateriaehdotukset (aamiainen, lounas, päivällinen)
 *  - GET /api/recipes/{id}   : yhden reseptin ainekset ja valmistusohjeet
 * Controller vain vastaanottaa parametrit ja kutsuu palvelua; varsinainen logiikka on service-kerroksessa.
 */
@RestController
public class RecipeController {

    private final RecipeSearchService recipeSearchService;
    private final RecipeDetailService recipeDetailService;

    public RecipeController(RecipeSearchService recipeSearchService, RecipeDetailService recipeDetailService) {
        this.recipeSearchService = recipeSearchService;
        this.recipeDetailService = recipeDetailService;
    }

    /**
     * Ehdottaa reseptejä yhdelle ateriale profiilin tavoitteiden pohjalta.
     * calories/protein/fat/carbs ovat PÄIVÄtavoitteita (kuten profiilisivulla).
     * Kalorit rajaavat hakua (päivätavoite / mealsPerDay, ± tolerance),
     * makrot vain järjestävät tulokset. Tulokset arvotaan parhaiden joukosta,
     * joten sama haku antaa joka kerta hieman eri ehdotuksia.
     * diets: vegetarian | vegan | gluten-free | dairy-free | high-protein | low-carb
     */
    @GetMapping("/api/recipes/search")
    public List<RecipeSummaryDto> search(
            // required = false: parametri on valinnainen, jolloin arvo on null eikä sillä rajata hakua.
            @RequestParam(required = false) Double calories,
            @RequestParam(required = false) Double protein,
            @RequestParam(required = false) Double fat,
            @RequestParam(required = false) Double carbs,
            @RequestParam(defaultValue = "3") int mealsPerDay,
            @RequestParam(defaultValue = "0.2") double tolerance,
            @RequestParam(required = false) Set<String> diets,
            @RequestParam(defaultValue = "10") int size
    ) {
        // Kootaan parametrit yhdeksi pyyntöolioksi, jonka service ymmärtää.
        RecipeSearchRequest request = new RecipeSearchRequest(
                calories, protein, fat, carbs, mealsPerDay, tolerance,
                diets == null ? Set.of() : diets
        );
        return recipeSearchService.search(request, size);
    }

    /**
     * Ehdottaa koko päivän ateriat: optionsPerMeal vaihtoehtoa aamiaiselle,
     * lounaalle ja päivälliselle. Päivän energia jaetaan aterioille
     * (aamiainen 25 %, lounas 35 %, päivällinen 40 %).
     */
    @GetMapping("/api/recipes/plan")
    public DayPlanDto plan(
            @RequestParam(required = false) Double calories,
            @RequestParam(required = false) Double protein,
            @RequestParam(required = false) Double fat,
            @RequestParam(required = false) Double carbs,
            @RequestParam(defaultValue = "0.2") double tolerance,
            @RequestParam(required = false) Set<String> diets,
            @RequestParam(defaultValue = "5") int optionsPerMeal
    ) {
        // mealsPerDay = 1, koska service jakaa päivän tavoitteen itse aterioille (25/35/40 %).
        RecipeSearchRequest request = new RecipeSearchRequest(
                calories, protein, fat, carbs, 1, tolerance,
                diets == null ? Set.of() : diets
        );
        return recipeSearchService.planDay(request, optionsPerMeal);
    }

    /** Yhden reseptin tiedot: ainekset ja valmistusohjeet vaiheittain (reseptin avaamisnäkymää varten). */
    @GetMapping("/api/recipes/{id}")
    public RecipeDetailDto detail(@PathVariable Integer id) {
        return recipeDetailService.find(id);
    }
}
