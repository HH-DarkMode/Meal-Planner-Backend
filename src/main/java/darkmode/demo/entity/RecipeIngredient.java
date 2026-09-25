package darkmode.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Yksi ainesosarivi reseptissä, esim. "6 cups cabbage, shredded".
 *  - quantity + unit_orig: alkuperäinen (amerikkalainen) määrä ja yksikkö
 *  - quantity_metric + unit_metric: muunnettu metrinen määrä (dl, rkl, tl, g, kpl)
 *  - food, grams ja ravintoarvokentät täytetään vain, kun ainesosa tunnistettiin
 *    JA määrä saatiin grammoiksi. Rivin food voi siis olla null, vaikka nimi on
 *    tunnistettu (ks. IngredientMatch), jos määrää ei voitu muuntaa.
 */
@Entity
@Table(name = "recipe_ingredients")
public class RecipeIngredient {

    // Keinotekoinen avain (tietokanta antaa numeron itse).
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Tämä puoli omistaa suhteen: recipe_id-sarake on tässä taulussa.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id")
    private Recipe recipe;

    // Rivin järjestysnumero reseptissä (1, 2, 3...), jotta ainekset pysyvät alkuperäisessä järjestyksessä.
    @Column(name = "line_no")
    private Integer lineNo;

    // Ainesosan nimi sellaisena kuin lähdedata sen antaa (ei sisällä määrää).
    @Column(name = "raw_text")
    private String rawText;

    // Siivottu nimi (pienet kirjaimet, täytesanat pois). Tällä haetaan ingredient_matches-taulusta.
    @Column(name = "ingredient_name_clean", nullable = false)
    private String ingredientNameClean;

    @Column(name = "quantity")
    private String quantity;

    @Column(name = "unit_orig")
    private String unitOrig;

    @Column(name = "quantity_metric")
    private BigDecimal quantityMetric;

    @Column(name = "unit_metric", length = 10)
    private String unitMetric;

    @Column(name = "misc")
    private String misc;

    // Matchattu Fineli-elintarvike (voi olla null, jos ainesosaa ei saatu yhdistettya)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "foodid")
    private Food food;

    @Column(name = "grams")
    private BigDecimal grams;

    @Column(name = "energia_kj")
    private Double energiaKj;

    @Column(name = "proteiini")
    private Double proteiini;

    @Column(name = "hiilihydraatti")
    private Double hiilihydraatti;

    @Column(name = "rasva")
    private Double rasva;

    public RecipeIngredient() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Recipe getRecipe() {
        return recipe;
    }

    public void setRecipe(Recipe recipe) {
        this.recipe = recipe;
    }

    public Integer getLineNo() {
        return lineNo;
    }

    public void setLineNo(Integer lineNo) {
        this.lineNo = lineNo;
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }

    public String getIngredientNameClean() {
        return ingredientNameClean;
    }

    public void setIngredientNameClean(String ingredientNameClean) {
        this.ingredientNameClean = ingredientNameClean;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public String getUnitOrig() {
        return unitOrig;
    }

    public void setUnitOrig(String unitOrig) {
        this.unitOrig = unitOrig;
    }

    public BigDecimal getQuantityMetric() {
        return quantityMetric;
    }

    public void setQuantityMetric(BigDecimal quantityMetric) {
        this.quantityMetric = quantityMetric;
    }

    public String getUnitMetric() {
        return unitMetric;
    }

    public void setUnitMetric(String unitMetric) {
        this.unitMetric = unitMetric;
    }

    public String getMisc() {
        return misc;
    }

    public void setMisc(String misc) {
        this.misc = misc;
    }

    public Food getFood() {
        return food;
    }

    public void setFood(Food food) {
        this.food = food;
    }

    public BigDecimal getGrams() {
        return grams;
    }

    public void setGrams(BigDecimal grams) {
        this.grams = grams;
    }

    public Double getEnergiaKj() {
        return energiaKj;
    }

    public void setEnergiaKj(Double energiaKj) {
        this.energiaKj = energiaKj;
    }

    public Double getProteiini() {
        return proteiini;
    }

    public void setProteiini(Double proteiini) {
        this.proteiini = proteiini;
    }

    public Double getHiilihydraatti() {
        return hiilihydraatti;
    }

    public void setHiilihydraatti(Double hiilihydraatti) {
        this.hiilihydraatti = hiilihydraatti;
    }

    public Double getRasva() {
        return rasva;
    }

    public void setRasva(Double rasva) {
        this.rasva = rasva;
    }
}
