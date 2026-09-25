package darkmode.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Elintarvikekohtainen mitta grammoiksi, esim. 1 dl sokeria = 85 g.
 * unit_code on Finelin koodi (DL, RKL, TL, KPL_M jne.) ja mass_g mitan paino grammoina.
 * Näillä reseptin määrä (esim. "2 rkl") muunnetaan grammoiksi ravintoarvojen laskentaa varten.
 */
@Entity
@Table(name = "food_units")
public class FoodUnit {

    // Keinotekoinen avain (tietokanta antaa numeron itse).
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "foodid")
    private Integer foodId;

    @Column(name = "unit_code", length = 20)
    private String unitCode;

    @Column(name = "unit_name_en")
    private String unitNameEn;

    // Mitan paino grammoina (ei ravintoarvo, siksi tarkka BigDecimal/NUMERIC eikä Double).
    @Column(name = "mass_g", nullable = false)
    private BigDecimal massG;

    public FoodUnit() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getFoodId() {
        return foodId;
    }

    public void setFoodId(Integer foodId) {
        this.foodId = foodId;
    }

    public String getUnitCode() {
        return unitCode;
    }

    public void setUnitCode(String unitCode) {
        this.unitCode = unitCode;
    }

    public String getUnitNameEn() {
        return unitNameEn;
    }

    public void setUnitNameEn(String unitNameEn) {
        this.unitNameEn = unitNameEn;
    }

    public BigDecimal getMassG() {
        return massG;
    }

    public void setMassG(BigDecimal massG) {
        this.massG = massG;
    }
}
