package darkmode.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Ainesosanimi -> Fineli-elintarvike -mappaus (build/06_finalize_matches.py:n tuottama).
 * method kertoo osuman luotettavuuden: curated | exact | fuzzy_NN | unmatched | curated_no_match.
 */
@Entity
@Table(name = "ingredient_matches")
public class IngredientMatch {

    @Id
    @Column(name = "ingredient_name")
    private String ingredientName;

    // Kuinka monessa reseptirivissä tämä ainesosanimi esiintyy (kertoo nimen tärkeyden).
    @Column(name = "occurrence_count", nullable = false)
    private Integer occurrenceCount;

    // Löydetty Fineli-elintarvike. null, jos ainesosaa ei saatu yhdistettyä mihinkään.
    // LAZY: Food ladataan tietokannasta vasta, kun sitä oikeasti tarvitaan.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "foodid")
    private Food food;

    @Column(name = "method", nullable = false, length = 20)
    private String method;

    @Column(name = "note")
    private String note;

    public IngredientMatch() {
    }

    public String getIngredientName() {
        return ingredientName;
    }

    public void setIngredientName(String ingredientName) {
        this.ingredientName = ingredientName;
    }

    public Integer getOccurrenceCount() {
        return occurrenceCount;
    }

    public void setOccurrenceCount(Integer occurrenceCount) {
        this.occurrenceCount = occurrenceCount;
    }

    public Food getFood() {
        return food;
    }

    public void setFood(Food food) {
        this.food = food;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
