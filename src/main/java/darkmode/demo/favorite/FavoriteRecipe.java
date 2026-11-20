package darkmode.demo.favorite;

import darkmode.demo.entity.Recipe;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

/**
 * Yksi käyttäjän suosikkiresepti (taulu favorite_recipes, ks. db/favorite-schema.sql).
 * Sydän (♥) Meal plan -sivulla tai reseptinäkymässä lisää rivin ja tyhjä sydän (♡) poistaa sen.
 * Sama resepti voi olla saman profiilin suosikeissa vain kerran (UNIQUE-ehto).
 */
@Entity
@Table(name = "favorite_recipes", uniqueConstraints = @UniqueConstraint(columnNames = { "profile_id", "recipe_id" }))
public class FavoriteRecipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Kenen suosikki: profiilin id (sama tapa kuin MealPlanSelection-luokassa). */
    @Column(name = "profile_id", nullable = false)
    private Long profileId;

    /**
     * Suosikkiresepti. LAZY = reseptiä ei haeta tietokannasta ennen kuin sitä tarvitaan
     * (esim. kun suosikkilista muutetaan JSON-vastaukseksi).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    /** Lisäysaika. Lista järjestetään tämän mukaan, uusin ensin. */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public FavoriteRecipe() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProfileId() {
        return profileId;
    }

    public void setProfileId(Long profileId) {
        this.profileId = profileId;
    }

    public Recipe getRecipe() {
        return recipe;
    }

    public void setRecipe(Recipe recipe) {
        this.recipe = recipe;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
