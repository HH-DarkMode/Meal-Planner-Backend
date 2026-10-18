package darkmode.demo.dto;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RecipeSearchRequestTest {

    @Test
    void appliesDefaultsForInvalidMealsPerDayAndTolerance() {
        RecipeSearchRequest request = new RecipeSearchRequest(2000.0, null, null, null, 0, 0, null);

        assertThat(request.mealsPerDay()).isEqualTo(3);
        assertThat(request.tolerance()).isEqualTo(0.2);
        assertThat(request.diets()).isEmpty();
    }

    @Test
    void keepsValidValuesAsGiven() {
        RecipeSearchRequest request = new RecipeSearchRequest(2000.0, 150.0, 70.0, 250.0, 4, 0.1, Set.of("vegan"));

        assertThat(request.mealsPerDay()).isEqualTo(4);
        assertThat(request.tolerance()).isEqualTo(0.1);
        assertThat(request.diets()).containsExactly("vegan");
    }
}
