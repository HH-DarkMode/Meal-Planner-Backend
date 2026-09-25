package darkmode.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Fineli-elintarvike ja sen ravintoarvot per 100 g (lähde: Fineli Rel20).
 * Ravintoarvot ovat Double (ei primitiivi double), koska ne voivat olla NULL:
 * osalla elintarvikkeista ei ole kaikkia arvoja, eikä "ei tiedossa" saa muuttua nollaksi.
 */
@Entity
@Table(name = "foods")
public class Food {

    // Finelin oma tunniste (FOODID). Sarakkeen nimi on ilman alaviivaa, kuten Finelissä.
    @Id
    @Column(name = "foodid")
    private Integer foodId;

    // Energia kilojouleina per 100 g. Fineli antaa energian vain kJ:na.
    @Column(name = "energia_kj")
    private Double energiaKj;

    // Sama energia kilokaloreina, laskettu: kcal = kJ / 4,184.
    @Column(name = "energia_kcal")
    private Double energiaKcal;

    // Proteiini, hiilihydraatti ja rasva grammoina per 100 g.
    @Column(name = "proteiini")
    private Double proteiini;

    @Column(name = "hiilihydraatti")
    private Double hiilihydraatti;

    @Column(name = "rasva")
    private Double rasva;

    public Food() {
    }

    public Integer getFoodId() {
        return foodId;
    }

    public void setFoodId(Integer foodId) {
        this.foodId = foodId;
    }

    public Double getEnergiaKj() {
        return energiaKj;
    }

    public void setEnergiaKj(Double energiaKj) {
        this.energiaKj = energiaKj;
    }

    public Double getEnergiaKcal() {
        return energiaKcal;
    }

    public void setEnergiaKcal(Double energiaKcal) {
        this.energiaKcal = energiaKcal;
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
