package darkmode.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Linkki elintarvikkeen ja sille sopivan erikoisruokavalion valilla.
 * Yhdella elintarvikkeella voi olla monta koodia (esim. VEGAN + GLUTFREE).
 */
@Entity
@Table(name = "food_specdiets")
public class FoodSpecDiet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "foodid")
    private Integer foodId;

    @Column(name = "code", length = 20)
    private String code;

    public FoodSpecDiet() {
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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
