package darkmode.demo.favorite;

import darkmode.demo.auth.User;
import darkmode.demo.auth.UserRepository;
import darkmode.demo.dto.RecipeSummaryDto;
import darkmode.demo.entity.Recipe;
import darkmode.demo.profile.Profile;
import darkmode.demo.profile.ProfileRepository;
import darkmode.demo.repository.RecipeRepository;
import darkmode.demo.service.RecipeSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FavoriteServiceTest {

    private FavoriteRecipeRepository favoriteRepository;
    private RecipeRepository recipeRepository;
    private UserRepository userRepository;
    private ProfileRepository profileRepository;
    private RecipeSearchService recipeSearchService;
    private FavoriteService service;

    @BeforeEach
    void setUp() {
        favoriteRepository = mock(FavoriteRecipeRepository.class);
        recipeRepository = mock(RecipeRepository.class);
        userRepository = mock(UserRepository.class);
        profileRepository = mock(ProfileRepository.class);
        recipeSearchService = mock(RecipeSearchService.class);
        service = new FavoriteService(favoriteRepository, recipeRepository, userRepository,
                profileRepository, recipeSearchService);
    }

    /** Uusi suosikki tallennetaan oikealle profiilille, oikealla reseptillä ja lisäysajalla. */
    @Test
    void addFavoriteSavesNewRowForOwnProfile() {
        givenUserWithProfile(42L, List.of());
        when(recipeRepository.findById(5)).thenReturn(Optional.of(recipe(5)));
        when(favoriteRepository.existsByProfileIdAndRecipe_RecipeId(42L, 5)).thenReturn(false);

        service.addFavorite(5, authentication("user-a"));

        // ArgumentCaptor nappaa talteen rivin, joka annettiin save-metodille
        ArgumentCaptor<FavoriteRecipe> saved = ArgumentCaptor.forClass(FavoriteRecipe.class);
        verify(favoriteRepository).save(saved.capture());
        assertThat(saved.getValue().getProfileId()).isEqualTo(42L);
        assertThat(saved.getValue().getRecipe().getRecipeId()).isEqualTo(5);
        assertThat(saved.getValue().getCreatedAt()).isNotNull();
    }

    /** Jos resepti on jo suosikeissa, uutta riviä ei tallenneta (ei tuplia). */
    @Test
    void addFavoriteTwiceDoesNotCreateDuplicate() {
        givenUserWithProfile(42L, List.of());
        when(recipeRepository.findById(5)).thenReturn(Optional.of(recipe(5)));
        when(favoriteRepository.existsByProfileIdAndRecipe_RecipeId(42L, 5)).thenReturn(true);

        service.addFavorite(5, authentication("user-a"));

        verify(favoriteRepository, never()).save(any());
    }

    /** Tuntematon resepti: virhe 404, eikä mitään tallenneta. */
    @Test
    void addFavoriteRejectsUnknownRecipe() {
        givenUserWithProfile(42L, List.of());
        when(recipeRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addFavorite(999, authentication("user-a")))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(favoriteRepository, never()).save(any());
    }

    /** Ilman profiilia suosikkia ei voi lisätä: virhe 400. */
    @Test
    void addFavoriteRequiresProfile() {
        givenUserWithoutProfile();

        assertThatThrownBy(() -> service.addFavorite(5, authentication("user-a")))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        verifyNoInteractions(favoriteRepository);
    }

    /** Poisto kohdistuu vain kirjautuneen käyttäjän omaan profiiliin (42). */
    @Test
    void removeFavoriteDeletesOnlyFromOwnProfile() {
        givenUserWithProfile(42L, List.of());

        service.removeFavorite(5, authentication("user-a"));

        verify(favoriteRepository).deleteByProfileIdAndRecipe_RecipeId(42L, 5);
    }

    /**
     * Haku palauttaa oman profiilin suosikit repositoryn järjestyksessä (uusin ensin).
     * Profiilin ruokavaliot (vegan) annetaan reseptin muunnokselle varoitusta ja vinkkiä varten.
     */
    @Test
    void getFavoritesReturnsOwnFavoritesWithDietWarningCheck() {
        givenUserWithProfile(42L, List.of("vegan"));
        FavoriteRecipe newer = favorite(7);
        FavoriteRecipe older = favorite(5);
        when(favoriteRepository.findByProfileIdOrderByCreatedAtDescIdDesc(42L)).thenReturn(List.of(newer, older));
        when(recipeSearchService.toSummary(newer.getRecipe(), Set.of("vegan"))).thenReturn(summary(7));
        when(recipeSearchService.toSummary(older.getRecipe(), Set.of("vegan"))).thenReturn(summary(5));

        List<RecipeSummaryDto> result = service.getFavorites(authentication("user-a"));

        assertThat(result).extracting(RecipeSummaryDto::recipeId).containsExactly(7, 5);
    }

    /** Ilman profiilia suosikkilista on tyhjä, eikä tietokannasta kysytä suosikkeja. */
    @Test
    void getFavoritesReturnsEmptyListWithoutProfile() {
        givenUserWithoutProfile();

        assertThat(service.getFavorites(authentication("user-a"))).isEmpty();
        verifyNoInteractions(favoriteRepository);
    }

    // ---- apumetodit (samat kuin MealPlanServiceTest-luokassa) ----

    private void givenUserWithProfile(long profileId, List<String> diets) {
        User user = new User();
        user.setUsername("user-a");
        Profile profile = new Profile();
        profile.setId(profileId);
        profile.setDiets(diets);
        when(userRepository.findByUsername("user-a")).thenReturn(Optional.of(user));
        // Testikäyttäjällä ei ole id:tä (null), koska sitä ei tallenneta oikeaan kantaan.
        when(profileRepository.findByUserId(null)).thenReturn(Optional.of(profile));
    }

    private void givenUserWithoutProfile() {
        User user = new User();
        user.setUsername("user-a");
        when(userRepository.findByUsername("user-a")).thenReturn(Optional.of(user));
        when(profileRepository.findByUserId(null)).thenReturn(Optional.empty());
    }

    private FavoriteRecipe favorite(int recipeId) {
        FavoriteRecipe favorite = new FavoriteRecipe();
        favorite.setProfileId(42L);
        favorite.setRecipe(recipe(recipeId));
        return favorite;
    }

    private Recipe recipe(int id) {
        Recipe recipe = new Recipe();
        recipe.setRecipeId(id);
        recipe.setName("Recipe " + id);
        return recipe;
    }

    private RecipeSummaryDto summary(int id) {
        return new RecipeSummaryDto(id, "Recipe " + id, null, null, null,
                null, null, null, null, null, List.of(), false, List.of());
    }

    private UsernamePasswordAuthenticationToken authentication(String username) {
        return new UsernamePasswordAuthenticationToken(username, null);
    }
}
