package darkmode.demo.mealplan;

import darkmode.demo.mealplan.MealSlots.MealSlot;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class MealSlotsTest {

    /** Jokaisella ateriamäärällä osuudet ovat yhteensä 100 %, eli päivän ehdotukset osuvat tavoitteeseen. */
    @Test
    void sharesSumTo100PercentForEveryMealCount() {
        for (int count = 1; count <= 8; count++) {
            double sum = MealSlots.forMealsPerDay(count).stream().mapToDouble(MealSlot::share).sum();
            // within: liukulukujen pyöristysvirhe sallitaan (esim. 0.9999999999)
            assertThat(sum).as("ateriamäärä %d", count).isCloseTo(1.0, within(1e-9));
        }
    }

    /** Ateriamäärällä n päivässä on täsmälleen n ateriaa. */
    @Test
    void returnsAsManyMealsAsRequested() {
        for (int count = 1; count <= 8; count++) {
            assertThat(MealSlots.forMealsPerDay(count)).as("ateriamäärä %d", count).hasSize(count);
        }
    }

    /** 4 ateriaa = aamupala, lounas, päivällinen ja iltapala (ei välipalaa). */
    @Test
    void fourMealsAreMainMealsAndSupper() {
        List<MealSlot> slots = MealSlots.forMealsPerDay(4);

        assertThat(slots).extracting(MealSlot::mealType)
                .containsExactly("Breakfast", "Lunch", "Dinner", "Supper");
        assertThat(slots).extracting(MealSlot::share)
                .containsExactly(0.25, 0.30, 0.30, 0.15);
    }

    /** Välipaloja ei ole, kun aterioita on 1–4. Viidennellä aterialla tulee ensimmäinen välipala. */
    @Test
    void snacksStartFromFiveMeals() {
        for (int count = 1; count <= 4; count++) {
            assertThat(MealSlots.forMealsPerDay(count))
                    .as("ateriamäärä %d", count)
                    .noneMatch(slot -> slot.recipeCategory().equals("Snack"));
        }
        assertThat(MealSlots.forMealsPerDay(5))
                .filteredOn(slot -> slot.recipeCategory().equals("Snack")).hasSize(1);
    }

    /** Puuttuva tai virheellinen ateriamäärä -> oletus 4 ateriaa. */
    @Test
    void missingOrInvalidMealCountDefaultsToFour() {
        assertThat(MealSlots.forMealsPerDay(null)).hasSize(4);
        assertThat(MealSlots.forMealsPerDay(0)).hasSize(4);
        assertThat(MealSlots.forMealsPerDay(9)).hasSize(4);
    }

    /** Ateriat ovat aina päivän kulun mukaisessa järjestyksessä (aamupala ensin, iltapala lopussa). */
    @Test
    void mealsAreInDayOrder() {
        for (int count = 1; count <= 8; count++) {
            List<Integer> positions = MealSlots.forMealsPerDay(count).stream()
                    .map(slot -> MealSlots.ALL_MEAL_TYPES.indexOf(slot.mealType()))
                    .toList();
            assertThat(positions).as("ateriamäärä %d", count).isSorted();
        }
    }

    /** 8 ateriaa: kaikki nimet mukana, joista 4 välipalaa (= maksimi). */
    @Test
    void eightMealsHaveFourSnacks() {
        List<MealSlot> slots = MealSlots.forMealsPerDay(8);

        assertThat(slots).extracting(MealSlot::mealType)
                .containsExactlyElementsOf(MealSlots.ALL_MEAL_TYPES);
        assertThat(slots).filteredOn(slot -> slot.recipeCategory().equals("Snack")).hasSize(4);
    }

    /** Iltapala haetaan aamupalareseptien joukosta (päätetty 7.11.2026). */
    @Test
    void supperUsesBreakfastRecipes() {
        MealSlot supper = MealSlots.forMealsPerDay(5).stream()
                .filter(slot -> slot.mealType().equals("Supper"))
                .findFirst().orElseThrow();

        assertThat(supper.recipeCategory()).isEqualTo("Breakfast");
    }

    /** Uudet nimet hyväksytään, tuntemattomat ei. */
    @Test
    void recognisesKnownMealTypes() {
        assertThat(MealSlots.isKnownMealType("Morning snack")).isTrue();
        assertThat(MealSlots.isKnownMealType("Snack")).isTrue();
        assertThat(MealSlots.isKnownMealType("Brunch")).isFalse();
        assertThat(MealSlots.isKnownMealType(null)).isFalse();
    }
}
