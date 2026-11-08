package darkmode.demo.mealplan;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MealPlanSelectionRepository extends JpaRepository<MealPlanSelection, Long> {

    Optional<MealPlanSelection> findByProfileIdAndPlanDateAndMealType(Long profileId, LocalDate planDate,
            String mealType);

    /**
     * Hakee profiilin kaikki valitut ateriat aikaväliltä from-to (molemmat päivät
     * mukana) päivämäärän mukaiseen järjestykseen. Kalenteri käyttää tätä
     * hakeakseen koko kuukauden kerralla. Spring muodostaa SQL-kyselyn
     * metodin nimestä: Between = väli, OrderByPlanDate = järjestys päivän mukaan.
     */
    List<MealPlanSelection> findByProfileIdAndPlanDateBetweenOrderByPlanDate(Long profileId, LocalDate from,
            LocalDate to);

    /** Päivän rivit tietyllä tilalla (esim. Accept all: päivän kaikki SUGGESTED-rivit). */
    List<MealPlanSelection> findByProfileIdAndPlanDateAndStatus(Long profileId, LocalDate planDate,
            MealPlanStatus status);

    /** Kalenteri: aikavälin rivit tietyllä tilalla (kalenteri hakee vain ACCEPTED). */
    List<MealPlanSelection> findByProfileIdAndPlanDateBetweenAndStatusOrderByPlanDate(Long profileId,
            LocalDate from, LocalDate to, MealPlanStatus status);

    /** Profiilimuutos: poistaa tämän päivän ja tulevien päivien rivit tietyllä tilalla (SUGGESTED). */
    void deleteByProfileIdAndPlanDateGreaterThanEqualAndStatus(Long profileId, LocalDate from,
            MealPlanStatus status);

    /**
     * Profiilimuutos: poistaa tämän päivän ja tulevien päivien rivit aterioilta, jotka eivät enää
     * kuulu käyttäjän päivään (esim. ateriamäärä 6 -> 4). Syötyjä (eaten = true) ei poisteta.
     */
    void deleteByProfileIdAndPlanDateGreaterThanEqualAndMealTypeNotInAndEatenFalse(Long profileId,
            LocalDate from, Collection<String> mealTypes);

}
