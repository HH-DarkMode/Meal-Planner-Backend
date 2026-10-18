package darkmode.demo.mealplan;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MealPlanSelectionRepository extends JpaRepository<MealPlanSelection, Long> {

    List<MealPlanSelection> findByProfileIdAndPlanDate(Long profileId, LocalDate planDate);

    Optional<MealPlanSelection> findByProfileIdAndPlanDateAndMealType(Long profileId, LocalDate planDate, String mealType);

    void deleteByProfileIdAndPlanDate(Long profileId, LocalDate planDate);
}
