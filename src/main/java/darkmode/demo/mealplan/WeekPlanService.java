package darkmode.demo.mealplan;

import darkmode.demo.dto.MealPlanEntryDto;
import darkmode.demo.dto.RecipeSearchRequest;
import darkmode.demo.dto.WeekPlanDto;
import darkmode.demo.entity.Recipe;
import darkmode.demo.mealplan.MealSlots.MealSlot;
import darkmode.demo.profile.Profile;
import darkmode.demo.service.RecipeSearchService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Viikkosuunnitelma: ehdotusten luonti, hyväksyntä ja sen peruminen, Vaihda ja Accept all.
 *
 * Säännöt (sovittu 7.11.2026):
 * - Ehdotukset luodaan, kun viikko avataan, ja ne TALLENNETAAN. Ne eivät vaihdu itsestään.
 * - Ehdotuksia luodaan vain tälle päivälle ja tuleville päiville, ei menneille.
 * - Syötyä ateriaa ei voi poistaa suunnitelmasta: ensin poistetaan syöty-merkintä.
 * - Hyväksyttyä ateriaa ei voi vaihtaa: ensin perutaan hyväksyntä.
 * - "Tämä päivä" (today) tulee aina selaimesta, jotta aikavyöhyke on oikea myös Rahdissa.
 */
@Service
public class WeekPlanService {

    private final MealPlanSelectionRepository selectionRepository;
    private final RecipeSearchService recipeSearchService;
    private final MealPlanService mealPlanService;

    public WeekPlanService(MealPlanSelectionRepository selectionRepository,
                           RecipeSearchService recipeSearchService,
                           MealPlanService mealPlanService) {
        this.selectionRepository = selectionRepository;
        this.recipeSearchService = recipeSearchService;
        this.mealPlanService = mealPlanService;
    }

    /**
     * Palauttaa viikon (start ... start + 6 päivää). Puuttuville aterioille luodaan ensin
     * ehdotukset (vain today ja tulevat päivät). Olemassa olevia rivejä ei muuteta.
     */
    @Transactional
    public WeekPlanDto getWeek(LocalDate start, LocalDate today, Authentication authentication) {
        Profile profile = mealPlanService.currentProfile(authentication).orElse(null);
        if (profile == null) {
            // Ei profiilia -> ei suunnitelmaa (frontend näyttää "Complete your profile").
            return new WeekPlanDto(start, List.of(), List.of());
        }
        LocalDate end = start.plusDays(6);
        List<MealSlot> slots = MealSlots.forMealsPerDay(profile.getMealsPerDay());

        List<MealPlanSelection> rows = new ArrayList<>(
                selectionRepository.findByProfileIdAndPlanDateBetweenOrderByPlanDate(profile.getId(), start, end));
        rows.addAll(createMissingSuggestions(profile, slots, rows, start, end, today));

        // Järjestys: päivä, sitten aterian paikka päivässä (aamupala ensin, iltapala lopussa).
        rows.sort(Comparator.comparing(MealPlanSelection::getPlanDate)
                .thenComparing(s -> MealSlots.ALL_MEAL_TYPES.indexOf(s.getMealType())));

        // Varoitus lasketaan käyttäjän NYKYISEN ruokavalion mukaan (ei tallenneta kantaan, ettei vanhene).
        boolean dietFilterActive = RecipeSearchService.hasIngredientBasedDiet(new HashSet<>(profile.getDiets()));
        return new WeekPlanDto(start,
                slots.stream().map(MealSlot::mealType).toList(),
                rows.stream().map(s -> toEntryWithWarning(s, dietFilterActive)).toList());
    }

    /**
     * Luo SUGGESTED-rivit aterioille, joilla ei vielä ole riviä. Haku tehdään kerran per ateria
     * koko viikolle (ei per päivä), joten 8 aterialla tehdään enintään 8 hakua 56:n sijaan.
     */
    private List<MealPlanSelection> createMissingSuggestions(Profile profile, List<MealSlot> slots,
            List<MealPlanSelection> existing, LocalDate start, LocalDate end, LocalDate today) {
        LocalDate firstDay = today.isAfter(start) ? today : start;
        if (firstDay.isAfter(end)) {
            return List.of(); // koko viikko on mennyt -> ei uusia ehdotuksia
        }
        // Mitkä (päivä, ateria) -paikat on jo täytetty, ja mitä reseptejä viikolla on jo käytetty.
        Set<String> filled = new HashSet<>();
        Set<Integer> usedRecipeIds = new HashSet<>();
        for (MealPlanSelection s : existing) {
            filled.add(key(s.getPlanDate(), s.getMealType()));
            usedRecipeIds.add(s.getRecipe().getRecipeId());
        }

        RecipeSearchRequest daily = dailyTargets(profile);
        Set<Integer> dietIds = null;
        boolean dietsResolved = false;
        List<MealPlanSelection> created = new ArrayList<>();

        for (MealSlot slot : slots) {
            List<LocalDate> missingDays = firstDay.datesUntil(end.plusDays(1))
                    .filter(day -> !filled.contains(key(day, slot.mealType())))
                    .toList();
            if (missingDays.isEmpty()) {
                continue;
            }
            // Hidas ruokavaliokysely ajetaan vain kerran ja vain, jos jotain pitää oikeasti hakea.
            if (!dietsResolved) {
                dietIds = recipeSearchService.resolveDietCompatibleIds(daily.diets());
                dietsResolved = true;
            }
            List<Recipe> recipes = recipeSearchService.findRecipesForMeal(
                    daily, slot, missingDays.size(), dietIds, usedRecipeIds);
            // Jos sopivia reseptejä on vähemmän kuin päiviä, loput päivät jäävät tyhjiksi (näytetään tyhjänä).
            for (int i = 0; i < recipes.size(); i++) {
                created.add(newSuggestion(profile.getId(), missingDays.get(i), slot.mealType(), recipes.get(i)));
                usedRecipeIds.add(recipes.get(i).getRecipeId());
            }
        }
        return selectionRepository.saveAll(created);
    }

    /** Hyväksy (accepted=true) tai peru hyväksyntä (accepted=false). */
    @Transactional
    public void setAccepted(LocalDate date, String mealType, boolean accepted, Authentication authentication) {
        MealPlanSelection row = findRow(requireProfile(authentication), date, mealType);
        if (!accepted && row.isEaten()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Syötyä ateriaa ei voi poistaa suunnitelmasta. Poista ensin syöty-merkintä.");
        }
        row.setStatus(accepted ? MealPlanStatus.ACCEPTED : MealPlanStatus.SUGGESTED);
        selectionRepository.save(row);
    }

    /** Accept all: päivän kaikki ehdotukset hyväksytyiksi. */
    @Transactional
    public void acceptDay(LocalDate date, Authentication authentication) {
        Profile profile = requireProfile(authentication);
        List<MealPlanSelection> suggestions = selectionRepository
                .findByProfileIdAndPlanDateAndStatus(profile.getId(), date, MealPlanStatus.SUGGESTED);
        suggestions.forEach(s -> s.setStatus(MealPlanStatus.ACCEPTED));
        selectionRepository.saveAll(suggestions);
    }

    /**
     * Vaihda: uusi ehdotus yhteen ateriaan. Sallittu vain ehdotukselle (ei hyväksytylle) ja vain
     * tälle päivälle tai tuleville päiville. Uusi resepti on eri kuin mikään muu viikon resepti.
     */
    @Transactional
    public MealPlanEntryDto reroll(LocalDate date, String mealType, LocalDate today, Authentication authentication) {
        if (date.isBefore(today)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Menneen päivän ateriaa ei voi vaihtaa");
        }
        Profile profile = requireProfile(authentication);
        MealPlanSelection row = findRow(profile, date, mealType);
        if (row.getStatus() == MealPlanStatus.ACCEPTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Hyväksyttyä ateriaa ei voi vaihtaa. Peru ensin hyväksyntä.");
        }
        MealSlot slot = MealSlots.forMealsPerDay(profile.getMealsPerDay()).stream()
                .filter(s -> s.mealType().equals(mealType))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Ateria ei kuulu käyttäjän päivään: " + mealType));

        // Viikon kaikki reseptit (myös nykyinen) jätetään pois, jotta tulee aidosti uusi resepti.
        LocalDate weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        Set<Integer> usedRecipeIds = selectionRepository
                .findByProfileIdAndPlanDateBetweenOrderByPlanDate(profile.getId(), weekStart, weekStart.plusDays(6))
                .stream().map(s -> s.getRecipe().getRecipeId()).collect(Collectors.toSet());

        RecipeSearchRequest daily = dailyTargets(profile);
        List<Recipe> recipes = recipeSearchService.findRecipesForMeal(daily, slot, 1,
                recipeSearchService.resolveDietCompatibleIds(daily.diets()), usedRecipeIds);
        if (recipes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Uutta sopivaa reseptiä ei löytynyt");
        }
        row.setRecipe(recipes.get(0));
        boolean dietFilterActive = RecipeSearchService.hasIngredientBasedDiet(daily.diets());
        return toEntryWithWarning(selectionRepository.save(row), dietFilterActive);
    }

    /** Käyttäjän päivän ateriat järjestyksessä (kalenteri tarvitsee nämä vaiheessa 7). */
    @Transactional(readOnly = true)
    public List<String> mealTypes(Authentication authentication) {
        Integer mealsPerDay = mealPlanService.currentProfile(authentication).map(Profile::getMealsPerDay).orElse(null);
        return MealSlots.forMealsPerDay(mealsPerDay).stream().map(MealSlot::mealType).toList();
    }

    /**
     * Profiilimuutos (sovittu 7.11.2026): tämän päivän ja tulevien päivien EHDOTUKSET poistetaan
     * (seuraava viikon avaus luo uudet), ja pois jääneiden aterioiden rivit poistetaan
     * (esim. 6 -> 4 ateriaa). Hyväksytyt ateriat, jotka kuuluvat yhä päivään, säilyvät.
     * Syötyihin ja menneisiin päiviin ei kosketa.
     */
    @Transactional
    public void resetFutureSuggestions(Profile profile, LocalDate today) {
        selectionRepository.deleteByProfileIdAndPlanDateGreaterThanEqualAndStatus(
                profile.getId(), today, MealPlanStatus.SUGGESTED);
        List<String> currentMealTypes = MealSlots.forMealsPerDay(profile.getMealsPerDay()).stream()
                .map(MealSlot::mealType).toList();
        selectionRepository.deleteByProfileIdAndPlanDateGreaterThanEqualAndMealTypeNotInAndEatenFalse(
                profile.getId(), today, currentMealTypes);
    }

    /**
     * Vaikuttaako profiilin muutos ehdotuksiin? Vain kalorit, makrot, ateriamäärä ja ruokavaliot.
     * Esim. painon tai nimen muutos ei vaihda ehdotuksia.
     */
    public static boolean affectsPlan(Profile before, Profile after) {
        return !Objects.equals(before.getCalories(), after.getCalories())
                || !Objects.equals(before.getProtein(), after.getProtein())
                || !Objects.equals(before.getFat(), after.getFat())
                || !Objects.equals(before.getCarbs(), after.getCarbs())
                || !Objects.equals(before.getMealsPerDay(), after.getMealsPerDay())
                // Ruokavaliot joukkona: järjestyksellä ei ole väliä (vegan, gluten-free = gluten-free, vegan).
                || !new HashSet<>(before.getDiets()).equals(new HashSet<>(after.getDiets()));
    }

    // ---- apumetodit ----

    /** Tietokannan rivi -> JSON-rivi diet-varoituksen kanssa (sama sääntö kuin Recipes-sivulla). */
    private MealPlanEntryDto toEntryWithWarning(MealPlanSelection s, boolean dietFilterActive) {
        return new MealPlanEntryDto(s.getPlanDate(), s.getMealType(), s.isEaten(),
                recipeSearchService.toSummary(s.getRecipe(), dietFilterActive), s.getStatus().name());
    }

    /** Profiilin päivätavoitteet hakupyynnöksi. mealsPerDay = 1, koska osuudet tulevat MealSlots-taulukosta. */
    private static RecipeSearchRequest dailyTargets(Profile profile) {
        return new RecipeSearchRequest(
                toDouble(profile.getCalories()), toDouble(profile.getProtein()),
                toDouble(profile.getFat()), toDouble(profile.getCarbs()),
                1, 0.2, new HashSet<>(profile.getDiets()));
    }

    private static Double toDouble(Integer value) {
        return value == null ? null : value.doubleValue();
    }

    private Profile requireProfile(Authentication authentication) {
        return mealPlanService.currentProfile(authentication)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tallenna profiili ensin"));
    }

    private MealPlanSelection findRow(Profile profile, LocalDate date, String mealType) {
        return selectionRepository.findByProfileIdAndPlanDateAndMealType(profile.getId(), date, mealType)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ateriaa ei löydy suunnitelmasta"));
    }

    private static MealPlanSelection newSuggestion(Long profileId, LocalDate date, String mealType, Recipe recipe) {
        MealPlanSelection s = new MealPlanSelection();
        s.setProfileId(profileId);
        s.setPlanDate(date);
        s.setMealType(mealType);
        s.setRecipe(recipe);
        s.setStatus(MealPlanStatus.SUGGESTED);
        return s;
    }

    private static String key(LocalDate date, String mealType) {
        return date + "|" + mealType;
    }
}
