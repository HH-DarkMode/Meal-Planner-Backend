package darkmode.demo.dto;

import java.util.Set;

/**
 * Profiilisivun tavoitteista rakennettu hakupyyntö.
 * calories/protein/fat/carbs ovat PÄIVÄtavoitteita (kuten Profile.tsx:ssa),
 * jotka jaetaan mealsPerDay:lla per-ateria-arvoiksi ja verrataan
 * toleranssilla reseptin per annos -arvoihin.
 */
public record RecipeSearchRequest(
        Double calories,
        Double protein,
        Double fat,
        Double carbs,
        int mealsPerDay,
        double tolerance,
        Set<String> diets
) {
    // Compact constructor: ajetaan aina kun olio luodaan. Korvaa puuttuvat/virheelliset arvot oletuksilla.
    public RecipeSearchRequest {
        if (mealsPerDay <= 0) mealsPerDay = 3;
        if (tolerance <= 0) tolerance = 0.2;
        if (diets == null) diets = Set.of();
    }
}
