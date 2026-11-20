package darkmode.demo.favorite;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Suosikkien tietokantahaut. Spring muodostaa SQL-kyselyn metodin nimestä.
 * "Recipe_RecipeId" tarkoittaa kenttää recipe.recipeId (alaviiva = mennään reseptin sisään).
 */
public interface FavoriteRecipeRepository extends JpaRepository<FavoriteRecipe, Long> {

    /** Profiilin kaikki suosikit, uusin ensin. Jos aika on sama, isompi id (myöhemmin lisätty) ensin. */
    List<FavoriteRecipe> findByProfileIdOrderByCreatedAtDescIdDesc(Long profileId);

    /** Onko resepti jo profiilin suosikeissa? (Ettei samaa lisätä kahdesti.) */
    boolean existsByProfileIdAndRecipe_RecipeId(Long profileId, Integer recipeId);

    /** Poistaa reseptin profiilin suosikeista. Jos sitä ei ole suosikeissa, ei tee mitään. */
    void deleteByProfileIdAndRecipe_RecipeId(Long profileId, Integer recipeId);
}
