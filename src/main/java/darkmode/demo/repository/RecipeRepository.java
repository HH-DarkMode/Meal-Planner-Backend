package darkmode.demo.repository;

import darkmode.demo.entity.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Reseptien tietokantakyselyt. Tämä on vain rajapinta: Spring Data JPA generoi toteutuksen itse.
 * JpaRepository antaa valmiit perusmetodit (findById, save, ...), ja @Query-annotaatiot
 * määrittelevät omat SQL-kyselyt. nativeQuery = true tarkoittaa tavallista SQL:ää (ei JPQL:ää).
 */
public interface RecipeRepository extends JpaRepository<Recipe, Integer> {

    /**
     * Hakee reseptit kaloriharaukan perusteella. Makrot (proteiini, rasva,
     * hiilihydraatit) eivat rajaa hakua, vaan niiden perusteella tulokset
     * jarjestetaan palvelukerroksessa. Kaikki rajat ovat valinnaisia
     * (null = ei rajoitusta) - tama on SQL:n ":param IS NULL OR ..." -kikka.
     * Kaytossa ovat valmiit per annos -arvot (kcal_per_serving jne.). Resepteilla,
     * joilta ne puuttuvat (NULL), ei ole luotettavaa ravintoarvoa, joten ne jaavat pois.
     */
    @Query(value = """
            SELECT * FROM recipes r
            WHERE r.kcal_per_serving IS NOT NULL
              AND r.kcal_per_serving > 0
              AND (:minCalories IS NULL OR r.kcal_per_serving >= :minCalories)
              AND (:maxCalories IS NULL OR r.kcal_per_serving <= :maxCalories)
              -- ateriatyyppi (esim. 'Breakfast'), null = kaikki
              AND (CAST(:category AS text) IS NULL OR r.category LIKE '%' || CAST(:category AS text) || '%')
              -- high-protein: vahintaan 25 % energiasta proteiinista
              AND (:highProtein = false OR r.protein_per_serving * 4 >= r.kcal_per_serving * 0.25)
              -- low-carb: enintaan 20 % energiasta hiilihydraateista
              AND (:lowCarb = false OR r.carbs_per_serving * 4 <= r.kcal_per_serving * 0.20)
            ORDER BY r.rating_value DESC NULLS LAST
            """, nativeQuery = true)
    List<Recipe> searchByCalories(
            @Param("minCalories") Double minCalories, @Param("maxCalories") Double maxCalories,
            @Param("category") String category,
            @Param("highProtein") boolean highProtein, @Param("lowCarb") boolean lowCarb
    );

    /**
     * Reseptit, jotka sopivat annettuun erikoisruokavalioon (specdiet-koodi).
     * Ainesosan Fineli-elintarvike haetaan nimen perusteella ingredient_matches-taulusta.
     * Resepti kelpaa, kun
     *  1. yksikaan tunnettu ainesosa ei riko ruokavaliota (kaikilla on koodi) ja
     *  2. vahintaan puolet reseptin riveista on tunnettuja (eli matchattu Fineliin).
     * Tuntemattomia rivejä voi siis olla, ja niista varoitetaan hakutuloksessa
     * (ks. findUnmatchedIngredientNames).
     */
    @Query(value = """
            SELECT r.recipe_id
            FROM recipes r
            WHERE r.total_lines > 0
              AND (SELECT count(*)
                   FROM recipe_ingredients ri
                   JOIN ingredient_matches im ON im.ingredient_name = ri.ingredient_name_clean
                   WHERE ri.recipe_id = r.recipe_id AND im.foodid IS NOT NULL
                  ) >= r.total_lines * 0.5
              AND NOT EXISTS (
                  SELECT 1
                  FROM recipe_ingredients ri
                  JOIN ingredient_matches im ON im.ingredient_name = ri.ingredient_name_clean
                  WHERE ri.recipe_id = r.recipe_id
                    AND im.foodid IS NOT NULL
                    AND NOT EXISTS (
                        SELECT 1 FROM food_specdiets fs
                        WHERE fs.foodid = im.foodid AND fs.code = :code
                    )
              )
            """, nativeQuery = true)
    List<Integer> findRecipeIdsCompatibleWithDiet(@Param("code") String code);

    /** Reseptin ainesosat, joita ei ole matchattu Fineliin (ruokavaliosopivuutta ei voi tarkistaa). */
    @Query(value = """
            SELECT ri.ingredient_name_clean
            FROM recipe_ingredients ri
            LEFT JOIN ingredient_matches im ON im.ingredient_name = ri.ingredient_name_clean
            WHERE ri.recipe_id = :recipeId AND im.foodid IS NULL
            ORDER BY ri.line_no
            """, nativeQuery = true)
    List<String> findUnmatchedIngredientNames(@Param("recipeId") Integer recipeId);
}
