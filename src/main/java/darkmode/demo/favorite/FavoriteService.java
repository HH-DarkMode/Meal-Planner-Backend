package darkmode.demo.favorite;

import darkmode.demo.auth.User;
import darkmode.demo.auth.UserRepository;
import darkmode.demo.dto.RecipeSummaryDto;
import darkmode.demo.entity.Recipe;
import darkmode.demo.profile.Profile;
import darkmode.demo.profile.ProfileRepository;
import darkmode.demo.repository.RecipeRepository;
import darkmode.demo.service.RecipeSearchService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Suosikkireseptit (My recipes): haku, lisäys ja poisto.
 * Suosikit tallennetaan kirjautuneen käyttäjän profiilille, joten jokainen näkee vain omansa.
 */
@Service
public class FavoriteService {

    private final FavoriteRecipeRepository favoriteRepository;
    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final RecipeSearchService recipeSearchService;

    public FavoriteService(
            FavoriteRecipeRepository favoriteRepository,
            RecipeRepository recipeRepository,
            UserRepository userRepository,
            ProfileRepository profileRepository,
            RecipeSearchService recipeSearchService) {
        this.favoriteRepository = favoriteRepository;
        this.recipeRepository = recipeRepository;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.recipeSearchService = recipeSearchService;
    }

    /**
     * Käyttäjän suosikit, uusin ensin. Jokaisessa on ravintoarvot ja diet-varoitus,
     * sama muoto kuin viikkonäkymän resepteissä (RecipeSummaryDto).
     * Ilman profiilia palautetaan tyhjä lista (sama tapa kuin kalenterissa).
     */
    @Transactional(readOnly = true)
    public List<RecipeSummaryDto> getFavorites(Authentication authentication) {
        Profile profile = currentProfile(authentication).orElse(null);
        if (profile == null) {
            return List.of();
        }
        // Varoitus ja vegaanin vinkki lasketaan profiilin ruokavalion mukaan. Ruokavalio voi muuttua,
        // joten ne lasketaan joka haulla.
        Set<String> diets = new HashSet<>(profile.getDiets());
        return favoriteRepository.findByProfileIdOrderByCreatedAtDescIdDesc(profile.getId()).stream()
                .map(favorite -> recipeSearchService.toSummary(favorite.getRecipe(), diets))
                .toList();
    }

    /**
     * Lisää reseptin suosikkeihin. Jos resepti on jo suosikeissa, mitään ei tapahdu
     * (sama tulos, vaikka nappia painettaisiin kahdesti).
     * Virheet: 400 = profiilia ei ole tallennettu, 404 = reseptiä ei löydy.
     */
    @Transactional
    public void addFavorite(Integer recipeId, Authentication authentication) {
        Long profileId = requireProfileId(authentication);
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reseptiä ei löydy"));
        if (favoriteRepository.existsByProfileIdAndRecipe_RecipeId(profileId, recipeId)) {
            return; // on jo suosikeissa
        }
        FavoriteRecipe favorite = new FavoriteRecipe();
        favorite.setProfileId(profileId);
        favorite.setRecipe(recipe);
        favorite.setCreatedAt(LocalDateTime.now());
        favoriteRepository.save(favorite);
    }

    /**
     * Poistaa reseptin suosikeista. Jos se ei ollut suosikeissa, mitään ei tapahdu.
     * Poisto koskee vain kirjautuneen käyttäjän omaa profiilia.
     */
    @Transactional
    public void removeFavorite(Integer recipeId, Authentication authentication) {
        Long profileId = requireProfileId(authentication);
        favoriteRepository.deleteByProfileIdAndRecipe_RecipeId(profileId, recipeId);
    }

    // ---- apumetodit ----

    /**
     * Kirjautuneen käyttäjän profiili (tyhjä, jos profiilia ei ole vielä tallennettu).
     * Sama haku kuin MealPlanService-luokassa. Se on toisessa paketissa (package-private),
     * joten kolme riviä toistetaan tässä, eikä vanhaa luokkaa tarvitse muuttaa.
     */
    private Optional<Profile> currentProfile(Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user no longer exists"));
        return profileRepository.findByUserId(user.getId());
    }

    /** Profiilin id. Jos profiilia ei ole, virhe 400 (suosikit tallennetaan profiilille). */
    private Long requireProfileId(Authentication authentication) {
        return currentProfile(authentication)
                .map(Profile::getId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tallenna profiili ensin"));
    }
}
