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

/**
 * Yksi ainesosarivi reseptissä, esim. "6 cups cabbage, shredded".
 * Reseptin avaamisnäkymä kokoaa rivin näistä osista: määrä, yksikkö, nimi ja lisätieto.
 * Taulussa on muitakin sarakkeita (metriset määrät, grammat, ravintoarvot, Fineli-tunniste),
 * mutta ne ovat vain ravintolaskennan välituloksia, eikä sovellus lue niitä.
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

    // Alkuperäinen (amerikkalainen) määrä ja yksikkö, esim. "6" ja "cups".
    @Column(name = "quantity")
    private String quantity;

    @Column(name = "unit_orig")
    private String unitOrig;

    // Lisätieto, esim. "shredded" tai "diced".
    @Column(name = "misc")
    private String misc;

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

    public String getMisc() {
        return misc;
    }

    public void setMisc(String misc) {
        this.misc = misc;
    }
}
