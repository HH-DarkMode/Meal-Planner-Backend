package darkmode.demo.service;

import darkmode.demo.dto.RecipeDetailDto;
import darkmode.demo.entity.Recipe;
import darkmode.demo.entity.RecipeIngredient;
import darkmode.demo.repository.RecipeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Yhden reseptin tiedot reseptin avaamisnäkymää varten (ainekset ja ohjeet).
 */
@Service
public class RecipeDetailService {

    private final RecipeRepository recipeRepository;

    public RecipeDetailService(RecipeRepository recipeRepository) {
        this.recipeRepository = recipeRepository;
    }

    // Transactional, koska ainesosat ladataan laiskasti (recipe.getIngredients()).
    @Transactional(readOnly = true)
    public RecipeDetailDto find(Integer recipeId) {
        Recipe r = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reseptia ei löydy"));

        return new RecipeDetailDto(
                r.getRecipeId(),
                r.getName(),
                r.getUrl(),
                r.getCategory(),
                r.getCuisine(),
                r.getServings() == null ? null : r.getServings().doubleValue(),
                r.getPrepTimeMin(),
                r.getCookTimeMin(),
                // Ravintoarvot ovat valmiiksi per annos. Voivat olla null, jos luotettavaa arvoa ei ole.
                r.getKcalPerServing(),
                r.getProteinPerServing(),
                r.getFatPerServing(),
                r.getCarbsPerServing(),
                r.getNutritionSource(),
                r.getIngredients().stream().map(RecipeDetailService::ingredientLine).toList(),
                splitInstructions(r.getInstructions())
        );
    }

    /** Kokoaa ainesosarivin muotoon "määrä yksikkö nimi, lisätieto", esim. "6 cups cabbage, shredded". */
    private static String ingredientLine(RecipeIngredient i) {
        List<String> parts = new ArrayList<>();
        for (String part : new String[]{i.getQuantity(), i.getUnitOrig(), i.getRawText()}) {
            if (part != null && !part.isBlank()) {
                parts.add(part.trim());
            }
        }
        String line = String.join(" ", parts);
        if (i.getMisc() != null && !i.getMisc().isBlank()) {
            line += ", " + i.getMisc().trim();
        }
        return line;
    }

    /** Tietokannassa ohjeet ovat yhtena tekstina, jossa vaiheet on erotettu merkinnalla " | ". */
    private static List<String> splitInstructions(String instructions) {
        if (instructions == null || instructions.isBlank()) {
            return List.of();
        }
        return Arrays.stream(instructions.split(" \\| ")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
