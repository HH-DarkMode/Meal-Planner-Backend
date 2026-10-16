package darkmode.demo.mealplan;

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

import java.time.LocalDate;

/**
 * Yksi käyttäjän reseptisivulla hyväksymä resepti tietylle päivälle ja ateriatyypille
 * (Breakfast/Lunch/Snack/Dinner). Uusi hyväksyntä samalle päivälle ja ateriatyypille
 * korvaa edellisen (ks. MealPlanService.selectMeal).
 */
@Entity
@Table(name = "meal_plan_selections",
        uniqueConstraints = @UniqueConstraint(columnNames = {"profile_id", "plan_date", "meal_type"}))
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

    public Recipe getRecipe() {
        return recipe;
    }

    public void setRecipe(Recipe recipe) {
        this.recipe = recipe;
    }
}
