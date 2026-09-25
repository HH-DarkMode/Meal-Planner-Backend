package darkmode.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Elintarvikkeen nimi tietyllä kielellä (FI tai EN). Yhdellä elintarvikkeella on siis kaksi riviä.
 * Tietokannassa on UNIQUE (foodid, lang), joten sama kieli ei voi esiintyä kahdesti.
 */
@Entity
@Table(name = "food_names")
public class FoodName {

    // Keinotekoinen avain. IDENTITY = tietokanta antaa numeron itse (BIGSERIAL).
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Viittaa Food-riviin. Pelkkä numero, ei relaatio-olio, jotta luokka pysyy yksinkertaisena.
    @Column(name = "foodid")
    private Integer foodId;

    @Column(name = "lang", length = 2)
    private String lang;

    @Column(name = "name", nullable = false)
    private String name;

    public FoodName() {
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

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
