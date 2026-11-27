package darkmode.demo.mealplan;

import darkmode.demo.auth.User;
import darkmode.demo.auth.UserRepository;
import darkmode.demo.dto.MealPlanEntryDto;
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
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Ateriasuunnitelman perustoiminnot: reseptin valinta päivälle (PUT /selections),
 * syöty-merkintä ja kalenterin haku (vain hyväksytyt). Viikkonäkymän logiikka on WeekPlanService-luokassa.
 * Valinnat tallennetaan kirjautuneen käyttäjän profiilille.
 */
@Service
public class MealPlanService {

    private final MealPlanSelectionRepository selectionRepository;
    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;

    /* tietojen haku max 62 pvän ajalta */
    private static final int MAX_RANGE_DAYS = 62;

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

    /**
     * Tallentaa reseptin annetun päivän ateriatyypille (oletus: tämä päivä).
     * Jos päivän ateriatyypille oli jo resepti, se korvataan.
     * Syödyn aterian reseptiä ei voi vaihtaa (virhe 409).
     */
    @Transactional
    public RecipeSummaryDto selectMeal(String mealType, Integer recipeId, LocalDate date,
            Authentication authentication) {
        // Sallitut ateriat tulevat MealSlots-taulukosta (8 nimeä, esim. "Morning snack").
        if (!MealSlots.isKnownMealType(mealType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tuntematon ateriatyyppi: " + mealType);
        }
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reseptia ei löydy"));
        Long profileId = currentProfileId(authentication);
        if (profileId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tallenna profiili ennen aterian valitsemista");
        }

        LocalDate planDate = date != null ? date : LocalDate.now();
        MealPlanSelection selection = selectionRepository
                .findByProfileIdAndPlanDateAndMealType(profileId, planDate, mealType)
                .orElseGet(MealPlanSelection::new);
        boolean recipeChanged = selection.getRecipe() == null
                || !recipeId.equals(selection.getRecipe().getRecipeId());
        // Syödyn aterian reseptiä ei voi vaihtaa: kalenterissa pitää säilyä se, mitä oikeasti syötiin.
        // Sama sääntö kuin hyväksynnän perumisessa (syöty ateria pysyy suunnitelmassa).
        // Saman reseptin valinta uudelleen on sallittu, koska se ei muuta mitään.
        if (selection.isEaten() && recipeChanged) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Syödyn aterian reseptiä ei voi vaihtaa. Poista ensin syöty-merkintä.");
        }
        selection.setProfileId(profileId);
        selection.setPlanDate(planDate);
        selection.setMealType(mealType);
        selection.setRecipe(recipe);
        // PUT /selections (esim. suosikin valinta suunnitelmaan): rivi on aina hyväksytty.
        // Jos samalla aterialla oli viikkonäkymän ehdotus, se muuttuu hyväksytyksi.
        selection.setStatus(MealPlanStatus.ACCEPTED);
        selectionRepository.save(selection);
        return toDto(recipe);
    }

    /**
     * Merkitsee suunnitellun aterian syödyksi (eaten = true) tai poistaa merkinnän (eaten = false).
     * Ateria haetaan kirjautuneen käyttäjän omasta suunnitelmasta päivän ja ateriatyypin mukaan.
     *
     * Virheet:
     *  - 400, jos käyttäjä ei ole vielä tallentanut profiilia
     *  - 404, jos päivän ateriaa ei ole suunnitelmassa
     *  - 409, jos ateria on vasta ehdotus (vain hyväksytyn aterian voi merkitä syödyksi)
     */
    @Transactional
    public void updateEatenStatus(LocalDate date, String mealType, boolean eaten, Authentication authentication) {
        MealPlanSelection plannedMeal = findPlannedMeal(requireProfileId(authentication), date, mealType);
        requireAccepted(plannedMeal);
        plannedMeal.setEaten(eaten);
        selectionRepository.save(plannedMeal);
    }

    /** Kirjautuneen käyttäjän profiilin id. Jos profiilia ei ole, virhe 400. */
    private Long requireProfileId(Authentication authentication) {
        Long profileId = currentProfileId(authentication);
        if (profileId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tallenna profiili ensin");
        }
        return profileId;
    }

    /** Käyttäjän suunnitelman ateria tiettynä päivänä. Jos sitä ei ole, virhe 404. */
    private MealPlanSelection findPlannedMeal(Long profileId, LocalDate date, String mealType) {
        return selectionRepository.findByProfileIdAndPlanDateAndMealType(profileId, date, mealType)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ateriaa ei löydy suunnitelmasta"));
    }

    /** Vain hyväksytyn aterian voi merkitä syödyksi (ehdotusta ei ole otettu suunnitelmaan). Muuten virhe 409. */
    private static void requireAccepted(MealPlanSelection plannedMeal) {
        if (!plannedMeal.isAccepted()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Vain hyväksytyn aterian voi merkitä syödyksi");
        }
    }

    /**
     * Kirjautuneen käyttäjän profiili (tyhjä, jos profiilia ei ole vielä tallennettu).
     * Näkyvyys "package-private" (ei private): WeekPlanService samassa paketissa käyttää tätä.
     */
    Optional<Profile> currentProfile(Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user no longer exists"));
        return profileRepository.findByUserId(user.getId());
    }

    private Long currentProfileId(Authentication authentication) {
        return currentProfile(authentication).map(Profile::getId).orElse(null);
    }

    /** Tietokannan rivi -> JSON-vastauksen rivi (päivä, ateria, syöty, resepti, tila). Kalenteri käyttää tätä. */
    static MealPlanEntryDto toEntryDto(MealPlanSelection s) {
        return new MealPlanEntryDto(s.getPlanDate(), s.getMealType(), s.isEaten(),
                toDto(s.getRecipe()), s.getStatus().name());
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
                false);
    }

    /**
     * Hakee kirjautuneen käyttäjän valitut ateriat aikaväliltä from-to (molemmat
     * päivät mukana). Palauttaa jokaisesta rivistä päivän, ateriatyypin,
     * syöty-tilan ja reseptin makrot kalenteria varten. Aikaväli saa olla enintään
     * MAX_RANGE_DAYS päivää, muuten palautetaan virhe 400. Jos käyttäjällä ei ole
     * profiilia, palautetaan tyhjä lista.
     */
    @Transactional(readOnly = true)
    public List<MealPlanEntryDto> getSelections(LocalDate from, LocalDate to, Authentication authentication) {
        if (to.isBefore(from) || ChronoUnit.DAYS.between(from, to) > MAX_RANGE_DAYS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Virheellinen aikaväli");
        }
        Long profileId = currentProfileId(authentication);
        if (profileId == null)
            return List.of();
        // Kalenteri näyttää vain hyväksytyt ateriat (ehdotukset eivät ole suunnitelmaa).
        return selectionRepository.findByProfileIdAndPlanDateBetweenAndStatusOrderByPlanDate(
                        profileId, from, to, MealPlanStatus.ACCEPTED)
                .stream()
                .map(MealPlanService::toEntryDto)
                .toList();
    }

}
