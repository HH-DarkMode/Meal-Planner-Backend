package darkmode.demo.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Resepti (lähde: Zenodo Culinary Recipes Dataset, Allrecipes-reseptejä).
 * Ravintoarvoja on kahdenlaisia:
 *  - src_*  = lähteen oma arvio PER ANNOS
 *  - calc_* = meidän Fineli-datasta laskema arvo KOKO RESEPTILLE (jaettava servings-luvulla)
 * Reseptin tekstit ovat englanniksi.
 */
@Entity
@Table(name = "recipes")
public class Recipe {

    @Id
    @Column(name = "recipe_id")
    private Integer recipeId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "author")
    private String author;

    @Column(name = "recipe_date")
    private LocalDate recipeDate;

    @Column(name = "rating_value")
    private BigDecimal ratingValue;

    @Column(name = "rating_count")
    private Integer ratingCount;

    @Column(name = "prep_time_min")
    private Integer prepTimeMin;

    @Column(name = "cook_time_min")
    private Integer cookTimeMin;

    // Merkkijono muodossa "['Dinner', 'Lunch']", josta ateriatyyppiä haetaan LIKE-vertailulla.
    @Column(name = "category")
    private String category;

    @Column(name = "cuisine")
    private String cuisine;

    // Annosmäärä. Tarvitaan, koska calc_*-arvot ovat koko reseptin summia.
    @Column(name = "servings")
    private BigDecimal servings;

    @Column(name = "num_steps")
    private Integer numSteps;

    // Valmistusohjeet yhtenä tekstinä, vaiheet erotettu merkinnällä " | ".
    @Column(name = "instructions")
    private String instructions;

    @Column(name = "url")
    private String url;

    // Alkuperaisen lahteen (AllRecipes) ilmoittamat arvot per annos
    @Column(name = "src_calories_kcal")
    private Double srcCaloriesKcal;

    @Column(name = "src_protein")
    private Double srcProtein;

    @Column(name = "src_fat")
    private Double srcFat;

    @Column(name = "src_carbs")
    private Double srcCarbs;

    // Fineli-pohjaisesti laskettu KOKO RESEPTIN ravintosisalto
    @Column(name = "calc_energia_kj")
    private Double calcEnergiaKj;

    @Column(name = "calc_energia_kcal")
    private Double calcEnergiaKcal;

    @Column(name = "calc_proteiini")
    private Double calcProteiini;

    @Column(name = "calc_hiilihydraatti")
    private Double calcHiilihydraatti;

    @Column(name = "calc_rasva")
    private Double calcRasva;

    // Kuinka moni ainesosarivi on mukana ravintoarvolaskennassa (rivillä on tunnettu
    // elintarvike ja määrä saatiin grammoiksi). Pieni osuus = laskettu arvo on epäluotettava.
    @Column(name = "matched_lines")
    private Integer matchedLines;

    @Column(name = "total_lines")
    private Integer totalLines;

    // Laskentaan mukaan päässeiden ainesosien yhteispaino grammoina.
    @Column(name = "matched_grams")
    private BigDecimal matchedGrams;

    // Valmiit ravintoarvot PER ANNOS, joita haku käyttää (täytetään tietokannassa, ks. db/recipe-nutrition.sql).
    // Lähteenä ensisijaisesti Allrecipesin oma arvio (nutritionSource = "source"), koska se kattaa
    // koko reseptin. Jos sitä ei ole, käytetään Fineli-laskentaa ("fineli") vain silloin, kun vähintään
    // 80 % ainesosariveistä on mukana. Muuten arvot ovat null eikä resepti tule ravintoarvohaun tuloksiin.
    @Column(name = "kcal_per_serving")
    private Double kcalPerServing;

    @Column(name = "protein_per_serving")
    private Double proteinPerServing;

    @Column(name = "fat_per_serving")
    private Double fatPerServing;

    @Column(name = "carbs_per_serving")
    private Double carbsPerServing;

    @Column(name = "nutrition_source")
    private String nutritionSource;

    // Reseptin ainesosarivit. mappedBy = suhteen omistaa RecipeIngredient.recipe.
    // LAZY: rivit ladataan vasta kun getIngredients() kutsutaan (haku ei tarvitse niitä).
    // cascade ALL + orphanRemoval: rivit kuuluvat reseptille ja poistuvat sen mukana.
    @OneToMany(mappedBy = "recipe", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNo ASC")
    private List<RecipeIngredient> ingredients = new ArrayList<>();

    public Recipe() {
    }

    public Integer getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(Integer recipeId) {
        this.recipeId = recipeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public LocalDate getRecipeDate() {
        return recipeDate;
    }

    public void setRecipeDate(LocalDate recipeDate) {
        this.recipeDate = recipeDate;
    }

    public BigDecimal getRatingValue() {
        return ratingValue;
    }

    public void setRatingValue(BigDecimal ratingValue) {
        this.ratingValue = ratingValue;
    }

    public Integer getRatingCount() {
        return ratingCount;
    }

    public void setRatingCount(Integer ratingCount) {
        this.ratingCount = ratingCount;
    }

    public Integer getPrepTimeMin() {
        return prepTimeMin;
    }

    public void setPrepTimeMin(Integer prepTimeMin) {
        this.prepTimeMin = prepTimeMin;
    }

    public Integer getCookTimeMin() {
        return cookTimeMin;
    }

    public void setCookTimeMin(Integer cookTimeMin) {
        this.cookTimeMin = cookTimeMin;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCuisine() {
        return cuisine;
    }

    public void setCuisine(String cuisine) {
        this.cuisine = cuisine;
    }

    public BigDecimal getServings() {
        return servings;
    }

    public void setServings(BigDecimal servings) {
        this.servings = servings;
    }

    public Integer getNumSteps() {
        return numSteps;
    }

    public void setNumSteps(Integer numSteps) {
        this.numSteps = numSteps;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Double getSrcCaloriesKcal() {
        return srcCaloriesKcal;
    }

    public void setSrcCaloriesKcal(Double srcCaloriesKcal) {
        this.srcCaloriesKcal = srcCaloriesKcal;
    }

    public Double getSrcProtein() {
        return srcProtein;
    }

    public void setSrcProtein(Double srcProtein) {
        this.srcProtein = srcProtein;
    }

    public Double getSrcFat() {
        return srcFat;
    }

    public void setSrcFat(Double srcFat) {
        this.srcFat = srcFat;
    }

    public Double getSrcCarbs() {
        return srcCarbs;
    }

    public void setSrcCarbs(Double srcCarbs) {
        this.srcCarbs = srcCarbs;
    }

    public Double getCalcEnergiaKj() {
        return calcEnergiaKj;
    }

    public void setCalcEnergiaKj(Double calcEnergiaKj) {
        this.calcEnergiaKj = calcEnergiaKj;
    }

    public Double getCalcEnergiaKcal() {
        return calcEnergiaKcal;
    }

    public void setCalcEnergiaKcal(Double calcEnergiaKcal) {
        this.calcEnergiaKcal = calcEnergiaKcal;
    }

    public Double getCalcProteiini() {
        return calcProteiini;
    }

    public void setCalcProteiini(Double calcProteiini) {
        this.calcProteiini = calcProteiini;
    }

    public Double getCalcHiilihydraatti() {
        return calcHiilihydraatti;
    }

    public void setCalcHiilihydraatti(Double calcHiilihydraatti) {
        this.calcHiilihydraatti = calcHiilihydraatti;
    }

    public Double getCalcRasva() {
        return calcRasva;
    }

    public void setCalcRasva(Double calcRasva) {
        this.calcRasva = calcRasva;
    }

    public Integer getMatchedLines() {
        return matchedLines;
    }

    public void setMatchedLines(Integer matchedLines) {
        this.matchedLines = matchedLines;
    }

    public Integer getTotalLines() {
        return totalLines;
    }

    public void setTotalLines(Integer totalLines) {
        this.totalLines = totalLines;
    }

    public BigDecimal getMatchedGrams() {
        return matchedGrams;
    }

    public void setMatchedGrams(BigDecimal matchedGrams) {
        this.matchedGrams = matchedGrams;
    }

    public Double getKcalPerServing() {
        return kcalPerServing;
    }

    public void setKcalPerServing(Double kcalPerServing) {
        this.kcalPerServing = kcalPerServing;
    }

    public Double getProteinPerServing() {
        return proteinPerServing;
    }

    public void setProteinPerServing(Double proteinPerServing) {
        this.proteinPerServing = proteinPerServing;
    }

    public Double getFatPerServing() {
        return fatPerServing;
    }

    public void setFatPerServing(Double fatPerServing) {
        this.fatPerServing = fatPerServing;
    }

    public Double getCarbsPerServing() {
        return carbsPerServing;
    }

    public void setCarbsPerServing(Double carbsPerServing) {
        this.carbsPerServing = carbsPerServing;
    }

    public String getNutritionSource() {
        return nutritionSource;
    }

    public void setNutritionSource(String nutritionSource) {
        this.nutritionSource = nutritionSource;
    }

    public List<RecipeIngredient> getIngredients() {
        return ingredients;
    }

    public void setIngredients(List<RecipeIngredient> ingredients) {
        this.ingredients = ingredients;
    }
}
