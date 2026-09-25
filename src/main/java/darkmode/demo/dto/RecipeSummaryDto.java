package darkmode.demo.dto;

import java.util.List;

/**
 * Reseptin ravintoarvot laskettuna PER ANNOS (koko reseptin calc_*-summat
 * jaettuna servings-maaralla), hakutuloksen nayttamista varten.
 *
 * nutritionSource: mistä ravintoarvot ovat: "source" (Allrecipesin oma arvio) tai "fineli" (meidän laskema).
 * matchedLines/totalLines: kuinka moni rivi on mukana ravintoarvolaskennassa.
 * unmatchedIngredients: ainesosat, joita ei tunnisteta Fineliin.
 * dietWarning: true, jos ruokavaliosuodatin oli paalla ja reseptissa on
 * tuntemattomia ainesosia, eli ruokavaliosopivuutta ei voi taata.
 */
public record RecipeSummaryDto(
        Integer recipeId,
        String name,
        String url,
        String category,
        String cuisine,
        Double servings,
        Integer prepTimeMin,
        Integer cookTimeMin,
        Double caloriesPerServing,
        Double proteinPerServing,
        Double fatPerServing,
        Double carbsPerServing,
        String nutritionSource,
        Integer matchedLines,
        Integer totalLines,
        List<String> unmatchedIngredients,
        boolean dietWarning
) {
}
