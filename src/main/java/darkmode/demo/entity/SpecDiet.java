package darkmode.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Fineli-erikoisruokavaliokoodi, esim. VEGAN, GLUTFREE, MILKFREE.
 */
@Entity
@Table(name = "specdiets")
public class SpecDiet {

    @Id
    @Column(name = "code", length = 20)
    private String code;

    @Column(name = "description_fi")
    private String descriptionFi;

    @Column(name = "description_en")
    private String descriptionEn;

    public SpecDiet() {
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescriptionFi() {
        return descriptionFi;
    }

    public void setDescriptionFi(String descriptionFi) {
        this.descriptionFi = descriptionFi;
    }

    public String getDescriptionEn() {
        return descriptionEn;
    }

    public void setDescriptionEn(String descriptionEn) {
        this.descriptionEn = descriptionEn;
    }
}
