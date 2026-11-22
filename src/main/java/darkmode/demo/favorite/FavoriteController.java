package darkmode.demo.favorite;

import darkmode.demo.dto.RecipeSummaryDto;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Suosikkien rajapinta (My recipes -sivu ja sydännapit). Vaatii kirjautumisen:
 * SecurityConfig sallii ilman kirjautumista vain /api/auth/**- ja Swagger-polut.
 *
 * Lisäys on PUT eikä POST, koska "tee tästä reseptistä suosikki" antaa saman tuloksen
 * vaikka sen lähettäisi monta kertaa.
 */
@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    /** GET /api/favorites: käyttäjän suosikit, uusin ensin. */
    @GetMapping
    public List<RecipeSummaryDto> getFavorites(Authentication authentication) {
        return favoriteService.getFavorites(authentication);
    }

    /**
     * PUT /api/favorites/{recipeId}: lisää suosikkeihin.
     *
     * Jos kaksi samaa pyyntöä tulee täsmälleen yhtä aikaa (esim. tuplaklikkaus), molemmat
     * voivat yrittää lisätä rivin, ja tietokannan UNIQUE-ehto hylkää toisen. Silloin resepti
     * on jo suosikeissa, joten virhe ohitetaan. Sama ajatus kuin MealPlanControllerin /week-haussa.
     */
    @PutMapping("/{recipeId}")
    public void addFavorite(@PathVariable Integer recipeId, Authentication authentication) {
        try {
            favoriteService.addFavorite(recipeId, authentication);
        } catch (DataIntegrityViolationException e) {
            // Rinnakkainen pyyntö ehti lisätä saman suosikin: lopputulos on oikea.
        }
    }

    /** DELETE /api/favorites/{recipeId}: poistaa suosikeista. */
    @DeleteMapping("/{recipeId}")
    public void removeFavorite(@PathVariable Integer recipeId, Authentication authentication) {
        favoriteService.removeFavorite(recipeId, authentication);
    }
}
