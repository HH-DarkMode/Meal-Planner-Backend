package darkmode.demo.dto;

import java.util.List;

/**
 * Hakutuloksen resepti: nimi, linkki, valmistusajat ja ravintoarvot PER ANNOS.
 *
 * nutritionSource: mistä ravintoarvot ovat: "source" (Allrecipesin oma arvio) tai "fineli" (meidän laskema).
 * unmatchedIngredients: ainesosat, joita ei tunnisteta Fineliin.
 * dietWarning: true, jos ruokavaliosuodatin oli paalla ja reseptissa on
 * tuntemattomia ainesosia, eli ruokavaliosopivuutta ei voi taata.
 */
public record RecipeSummaryDto(
        Integer recipeId,
        String name,
        String url,
        Integer prepTimeMin,
        Integer cookTimeMin,
        Double caloriesPerServing,
        Double proteinPerServing,
        Double fatPerServing,
        Double carbsPerServing,
        String nutritionSource,
        List<String> unmatchedIngredients,
        boolean dietWarning
) {
}
