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
     *  1. yksikään tunnettu ainesosa ei riko ruokavaliota (kaikilla on koodi),
     *  2. vähintään puolet reseptin riveistä on tunnettuja (eli matchattu Fineliin) ja
     *  3. yhdenkään tuntemattoman ainesosan nimessä ei ole ruokavaliolle estettyä sanaa
     *     (blockedPattern, esim. "skinless" tai "italian sausage"), ellei nimessä ole myös
     *     sallittua sanaa (allowedPattern, esim. "vegan"). blockedPattern null = ei estoa.
     *  4. reseptin nimessä ei ole estettyä sanaa samalla säännöllä (esim. "Spaghetti and Meatballs").
     *     Sanalistat: ks. DietIngredientRules.
     * Muita tuntemattomia rivejä voi olla, ja niistä varoitetaan hakutuloksessa
     * (ks. findIngredientNames). ~* = vastaa kuviota (isot ja pienet kirjaimet samoin), !~* = ei vastaa.
     *
     * Ainesosarivit lasketaan kertaalleen GROUP BY:llä (ingredient_info) sen sijaan,
     * että jokaiselle reseptille ajettaisiin oma korreloitu aliohjelma — alkuperäinen
     * muotoilu kesti n. 5 s/ruokavalio 50 000 reseptin kannassa, tämä n. 0,7 s samalla
     * tulosjoukolla (varmistettu EXPLAIN ANALYZE + tulosjoukkovertailulla).
     */
    @Query(value = """
            WITH ingredient_info AS (
                SELECT ri.recipe_id,
                       count(*) FILTER (WHERE im.foodid IS NOT NULL) AS matched_count,
                       bool_or(im.foodid IS NOT NULL AND NOT EXISTS (
                           SELECT 1 FROM food_specdiets fs
                           WHERE fs.foodid = im.foodid AND fs.code = :code
                       )) AS has_incompatible,
                       bool_or(im.foodid IS NULL
                           AND CAST(:blockedPattern AS text) IS NOT NULL
                           AND ri.ingredient_name_clean ~* CAST(:blockedPattern AS text)
                           AND ri.ingredient_name_clean !~* CAST(:allowedPattern AS text)
                       ) AS has_blocked_unknown
                FROM recipe_ingredients ri
                LEFT JOIN ingredient_matches im ON im.ingredient_name = ri.ingredient_name_clean
                GROUP BY ri.recipe_id
            )
            SELECT r.recipe_id
            FROM recipes r
            JOIN ingredient_info ii ON ii.recipe_id = r.recipe_id
            WHERE r.total_lines > 0
              AND ii.matched_count >= r.total_lines * 0.5
              AND NOT COALESCE(ii.has_incompatible, false)
              AND NOT COALESCE(ii.has_blocked_unknown, false)
              -- reseptin nimessä estetty sana (esim. "Spaghetti and Meatballs"), ellei myös sallittua (esim. "Vegan")
              AND NOT (CAST(:blockedPattern AS text) IS NOT NULL
                       AND r.name ~* CAST(:blockedPattern AS text)
                       AND r.name !~* CAST(:allowedPattern AS text))
            """, nativeQuery = true)
    List<Integer> findRecipeIdsCompatibleWithDiet(@Param("code") String code,
            @Param("blockedPattern") String blockedPattern, @Param("allowedPattern") String allowedPattern);

    /** Reseptin ainesosan nimi ja tieto, onko se tunnistettu Finelistä. */
    interface IngredientName {
        String getIngredient();

        Boolean getRecognized();
    }

    /**
     * Reseptin kaikki ainesosat rivijärjestyksessä. Tunnistamattomista tulee ⚠-varoitus, ja
     * maitotuotteista ja kananmunasta vegaanille korvausvinkki (myös tunnistetuista, esim. "butter").
     * IngredientName on projektio: Spring täyttää sen sarakkeista ingredient ja recognized.
     */
    @Query(value = """
            SELECT ri.ingredient_name_clean AS ingredient,
                   CASE WHEN im.foodid IS NULL THEN false ELSE true END AS recognized
            FROM recipe_ingredients ri
            LEFT JOIN ingredient_matches im ON im.ingredient_name = ri.ingredient_name_clean
            WHERE ri.recipe_id = :recipeId
            ORDER BY ri.line_no
            """, nativeQuery = true)
    List<IngredientName> findIngredientNames(@Param("recipeId") Integer recipeId);
}
