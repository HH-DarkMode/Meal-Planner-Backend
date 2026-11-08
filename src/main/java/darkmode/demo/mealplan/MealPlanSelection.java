package darkmode.demo.mealplan;

import darkmode.demo.entity.Recipe;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;

/**
 * Yksi rivi käyttäjän ateriasuunnitelmassa: tietyn päivän tietty ateria
 * (esim. Breakfast, Lunch, Snack, Dinner) ja sille valittu resepti.
 * Rivi on joko järjestelmän ehdotus (SUGGESTED) tai käyttäjän hyväksymä
 * ateria (ACCEPTED), ks. MealPlanStatus.
 * Samalle päivälle ja ateriatyypille voi olla vain yksi rivi: uusi valinta
 * korvaa edellisen (ks. MealPlanService.selectMeal).
 */
@Entity
@Table(name = "meal_plan_selections", uniqueConstraints = @UniqueConstraint(columnNames = { "profile_id", "plan_date",
        "meal_type" }))
public class MealPlanSelection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "profile_id", nullable = false)
    private Long profileId;

    @Column(name = "plan_date", nullable = false)
    private LocalDate planDate;

    @Column(name = "meal_type", nullable = false)
    private String mealType;

    @Column(name = "eaten", nullable = false)
    private boolean eaten;

    /**
     * Rivin tila: ehdotus (SUGGESTED) vai hyväksytty (ACCEPTED).
     *
     * @Enumerated(EnumType.STRING) tallentaa tietokantaan arvon nimen tekstinä
     * ("SUGGESTED" / "ACCEPTED"). Oletus olisi järjestysnumero (0 / 1), jota on
     * vaikea lukea kannasta ja joka menisi sekaisin, jos arvojen järjestys muuttuu.
     *
     * Oletusarvo ACCEPTED: kun koodi luo uuden rivin (esim. Recipes-sivun
     * "Add to meal plan"), se on hyväksytty kuten tähänkin asti. Näin vanha
     * toiminta ei muutu tässä vaiheessa.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MealPlanStatus status = MealPlanStatus.ACCEPTED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    public MealPlanSelection() {
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

    public LocalDate getPlanDate() {
        return planDate;
    }

    public void setPlanDate(LocalDate planDate) {
        this.planDate = planDate;
    }

    public String getMealType() {
        return mealType;
    }

    public void setMealType(String mealType) {
        this.mealType = mealType;
    }

    public boolean isEaten() {
        return eaten;
    }

    public void setEaten(boolean eaten) {
        this.eaten = eaten;
    }

    public MealPlanStatus getStatus() {
        return status;
    }

    public void setStatus(MealPlanStatus status) {
        this.status = status;
    }

    public Recipe getRecipe() {
        return recipe;
    }

    public void setRecipe(Recipe recipe) {
        this.recipe = recipe;
    }
}
