package darkmode.demo.service;

import darkmode.demo.dto.DayPlanDto;
import darkmode.demo.dto.RecipeSearchRequest;
import darkmode.demo.dto.RecipeSummaryDto;
import darkmode.demo.entity.Recipe;
import darkmode.demo.repository.RecipeRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reseptien haku ja ateriaehdotukset profiilin tavoitteiden pohjalta.
 * Ajatus: kalorit rajaavat hakua, makrot (proteiini, rasva, hiilihydraatit) vain
 * järjestävät tulokset, erikoisruokavaliot karsivat sopimattomat ja lopuksi
 * parhaiden joukosta arvotaan, jotta ehdotukset vaihtelevat.
 */
@Service
public class RecipeSearchService {

    /**
     * Profiilisivun ruokavaliovalinta -> Fineli specdiet-koodi.
     * "high-protein" ja "low-carb" puuttuvat tästä tarkoituksella: ne eivät ole ainesosakohtaisia
     * luokituksia vaan reseptin makrosuhteita, ja ne tarkistetaan SQL-kyselyssä.
     */
    private static final Map<String, String> DIET_TO_SPECDIET_CODE = Map.of(
            "vegetarian", "LACOVEGE",
            "vegan", "VEGAN",
            "gluten-free", "GLUTFREE",
            "dairy-free", "MILKFREE"
    );

    /** Kuinka monesta parhaasta osumasta arvotaan lopulliset ehdotukset. */
    private static final int TOP_POOL_SIZE = 30;

    /** Kuinka suuri osa paivan energiasta kullekin aterialle (yhteensa 100 %). */
    private static final double BREAKFAST_SHARE = 0.25;
    private static final double LUNCH_SHARE = 0.30;
    private static final double SNACK_SHARE = 0.15;
    private static final double DINNER_SHARE = 0.30;

    private final RecipeRepository recipeRepository;

    public RecipeSearchService(RecipeRepository recipeRepository) {
        this.recipeRepository = recipeRepository;
    }

    /** Yksittainen haku: palauttaa size kpl arvottuja ehdotuksia parhaiden osumien joukosta. */
    public List<RecipeSummaryDto> search(RecipeSearchRequest request, int size) {
        return findSuggestions(request, null, size);
    }

    /** Paivan ehdotukset: optionsPerMeal vaihtoehtoa aamiaiselle, lounaalle, valipalalle ja paivalliselle. */
    public DayPlanDto planDay(RecipeSearchRequest daily, int optionsPerMeal) {
        return new DayPlanDto(
                findMealOptions(daily, "Breakfast", BREAKFAST_SHARE, optionsPerMeal),
                findMealOptions(daily, "Lunch", LUNCH_SHARE, optionsPerMeal),
                findMealOptions(daily, "Snack", SNACK_SHARE, optionsPerMeal),
                findMealOptions(daily, "Dinner", DINNER_SHARE, optionsPerMeal)
        );
    }

    /** Skaalaa paivatavoitteen yhden aterian osuuteen ja hakee sen ateriatyypin reseptit. */
    private List<RecipeSummaryDto> findMealOptions(RecipeSearchRequest daily, String category,
                                                   double share, int count) {
        RecipeSearchRequest meal = new RecipeSearchRequest(
                scale(daily.calories(), share), scale(daily.protein(), share),
                scale(daily.fat(), share), scale(daily.carbs(), share),
                1, daily.tolerance(), daily.diets()
        );
        return findSuggestions(meal, category, count);
    }

    private static Double scale(Double value, double share) {
        return value == null ? null : value * share;
    }

    /**
     * Haun vaiheet:
     * 1. Kalorit rajaavat (tietokannassa), makrot eivat rajaa.
     * 2. Erikoisruokavaliot karsivat reseptit, jotka eivat sovi (tuntemattomat
     *    ainesosat sallitaan, mutta niista tulee varoitus tulokseen).
     * 3. Jarjestetaan makrojen mukaan: lahinna tavoitesuhdetta ensin.
     * 4. Parhaiden joukosta arvotaan count kpl, jotta ehdotukset vaihtelevat.
     */
    private List<RecipeSummaryDto> findSuggestions(RecipeSearchRequest request, String category, int count) {
        // 1. Kaloriväli per ateria: päivätavoite / aterioiden määrä, plusmiinus toleranssi.
        //    null tarkoittaa "ei rajoitusta" (jos tavoitetta ei annettu).
        Double minCalories = null;
        Double maxCalories = null;
        if (request.calories() != null) {
            double perMeal = request.calories() / request.mealsPerDay();
            minCalories = perMeal * (1 - request.tolerance());
            maxCalories = perMeal * (1 + request.tolerance());
        }

        // Haku tietokannasta: kalorit, ateriatyyppi sekä high-protein/low-carb -ehdot.
        List<Recipe> recipes = recipeRepository.searchByCalories(
                minCalories, maxCalories, category,
                request.diets().contains("high-protein"),
                request.diets().contains("low-carb")
        );

        // 2. Erikoisruokavaliot: pidetään vain reseptit, joiden id on sopivien joukossa.
        //    null = yhtään ruokavaliorajausta ei pyydetty, jolloin kaikki reseptit kelpaavat.
        Set<Integer> dietCompatibleIds = resolveDietCompatibleIds(request.diets());
        if (dietCompatibleIds != null) {
            recipes = recipes.stream()
                    .filter(r -> dietCompatibleIds.contains(r.getRecipeId()))
                    .toList();
        }

        // 3. Järjestys makrojen mukaan (pienin etäisyys tavoitteeseen ensin) ja otetaan parhaat.
        //    Math.max varmistaa, että poolissa on vähintään count reseptiä.
        //    new ArrayList<>(...): toList() palauttaa muuttumattoman listan, mutta shuffle vaatii muutettavan.
        List<Recipe> topPool = new ArrayList<>(recipes.stream()
                .sorted(Comparator.comparingDouble(r -> macroDistance(r, request)))
                .limit(Math.max(TOP_POOL_SIZE, count))
                .toList());
        // 4. Arvonta: sama haku antaa joka kerta hieman eri ehdotuksia.
        Collections.shuffle(topPool);

        // Varoitus näytetään vain, jos ruokavaliosuodatin oli käytössä.
        boolean dietFilterActive = dietCompatibleIds != null;
        return topPool.stream().limit(count).map(r -> toDto(r, dietFilterActive)).toList();
    }

    /**
     * Kuinka kaukana reseptin makrojakauma on tavoitteesta (pienempi = parempi).
     * Verrataan osuuksia energiasta, ei grammoja: esim. "24 % energiasta
     * proteiinista". Energiakertoimet: proteiini 4, hiilihydraatti 4, rasva 9 kcal/g.
     * Ilman kalorien tavoitetta osuuksia ei voi laskea, jolloin kaikki reseptit ovat tasavertaisia.
     */
    private static double macroDistance(Recipe r, RecipeSearchRequest target) {
        if (target.calories() == null) {
            return 0;
        }
        double kcal = r.getKcalPerServing();
        double distance = 0;
        if (target.protein() != null) {
            distance += Math.abs(r.getProteinPerServing() * 4 / kcal - target.protein() * 4 / target.calories());
        }
        if (target.fat() != null) {
            distance += Math.abs(r.getFatPerServing() * 9 / kcal - target.fat() * 9 / target.calories());
        }
        if (target.carbs() != null) {
            distance += Math.abs(r.getCarbsPerServing() * 4 / kcal - target.carbs() * 4 / target.calories());
        }
        return distance;
    }

    /**
     * Palauttaa reseptien id-joukon, jotka sopivat KAIKKIIN pyydettyihin
     * specdiet-pohjaisiin ruokavalioihin (vegaani, gluteeniton, ...) leikkaamalla
     * joukot. Palauttaa null jos yhtaan specdiet-pohjaista ruokavaliota ei
     * pyydetty (= ei rajoitusta).
     */
    private Set<Integer> resolveDietCompatibleIds(Set<String> diets) {
        Set<Integer> result = null;
        for (String dietId : diets) {
            String code = DIET_TO_SPECDIET_CODE.get(dietId);
            if (code == null) {
                continue;
            }
            Set<Integer> idsForCode = new HashSet<>(recipeRepository.findRecipeIdsCompatibleWithDiet(code));
            if (result == null) {
                result = idsForCode;
            } else {
                result.retainAll(idsForCode);
            }
        }
        return result;
    }

    private RecipeSummaryDto toDto(Recipe r, boolean dietFilterActive) {
        // Yksi lisäkysely per palautettava resepti (enintään size kpl), joten nopeus riittää.
        List<String> unmatched = recipeRepository.findUnmatchedIngredientNames(r.getRecipeId());
        return new RecipeSummaryDto(
                r.getRecipeId(),
                r.getName(),
                r.getUrl(),
                r.getPrepTimeMin(),
                r.getCookTimeMin(),
                // Ravintoarvot ovat valmiiksi per annos (haku on jo karsinut reseptit ilman niitä).
                round(r.getKcalPerServing()),
                round(r.getProteinPerServing()),
                round(r.getFatPerServing()),
                round(r.getCarbsPerServing()),
                r.getNutritionSource(),
                unmatched,
                dietFilterActive && !unmatched.isEmpty()
        );
    }

    /** Pyöristää yhteen desimaaliin (esim. 723,84 -> 723,8). */
    private static Double round(Double value) {
        if (value == null) return null;
        return Math.round(value * 10) / 10.0;
    }
}
