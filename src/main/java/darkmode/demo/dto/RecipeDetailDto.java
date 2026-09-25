package darkmode.demo.dto;

import java.util.List;

/**
 * Yhden reseptin tiedot reseptin avaamisnakymaa varten: ravintoarvot per annos,
 * ainekset valmiiksi muotoiltuina riveina seka valmistusohjeet vaiheittain.
 * Ainekset ja ohjeet ovat englanniksi (lahteena Allrecipes-aineisto).
 */
public record RecipeDetailDto(
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
        List<String> ingredients,
        List<String> instructions
) {
}
