package darkmode.demo.mealplan;

import java.util.List;
import java.util.Map;

/**
 * Päivän ateriat ja niiden energiaosuudet ateriamäärän mukaan (profiilin "Meals per day", 1–8).
 *
 * Esimerkki: 4 ateriaa ja päivätavoite 2000 kcal
 *   Breakfast 25 % = 500 kcal, Lunch 30 % = 600 kcal, Dinner 30 % = 600 kcal, Supper 15 % = 300 kcal.
 * Osuus vaikuttaa vain siihen, minkä kokoisia reseptejä aterialle EHDOTETAAN.
 * Kalenterin summat lasketaan aina valittujen reseptien todellisista arvoista.
 *
 * Jokaisella aterialla on oma nimi (ei järjestysnumeroa), joten samana päivänä voi olla
 * useita välipaloja: Morning snack, Snack, Evening snack ja Late snack. Nimi tallennetaan
 * tietokannan meal_type-sarakkeeseen. Välipaloja on enintään 4.
 *
 * Kaikki taulukon luvut ovat tässä yhdessä paikassa, joten niitä on helppo muuttaa myöhemmin.
 */
public final class MealSlots {

    // Ateriannimet vakioina, jotta kirjoitusvirhe (esim. "Breakfest") huomataan jo käännösvaiheessa.
    // Breakfast, Lunch, Snack ja Dinner ovat samat kuin ennen, joten vanha data ja Recipes-sivu toimivat.
    public static final String BREAKFAST = "Breakfast";
    public static final String MORNING_SNACK = "Morning snack";
    public static final String LUNCH = "Lunch";
    public static final String SNACK = "Snack"; // iltapäivän välipala (frontend näyttää nimellä "Afternoon snack", jos välipaloja on useita)
    public static final String DINNER = "Dinner";
    public static final String EVENING_SNACK = "Evening snack";
    public static final String SUPPER = "Supper"; // iltapala
    public static final String LATE_SNACK = "Late snack";

    /** Kaikki ateriat päivän kulun mukaisessa järjestyksessä. Näytetään aina tässä järjestyksessä. */
    public static final List<String> ALL_MEAL_TYPES = List.of(
            BREAKFAST, MORNING_SNACK, LUNCH, SNACK, DINNER, EVENING_SNACK, SUPPER, LATE_SNACK);

    /** Ateriamäärä, jota käytetään, jos profiilissa ei ole arvoa tai arvo on sallitun välin ulkopuolella. */
    public static final int DEFAULT_MEALS_PER_DAY = 4;

    /**
     * Yksi päivän ateria.
     *
     * @param mealType       aterian nimi, esim. "Lunch" (tallennetaan meal_type-sarakkeeseen)
     * @param share          osuus päivän energiasta, esim. 0.30 = 30 %
     * @param recipeCategory reseptikannan kategoria, josta ehdotukset haetaan. Haku on muotoa
     *                       "kategoria sisältää tekstin", joten "Snack" löytää myös "Appetizer, Snack" -reseptit
     *                       ja "Lunch" myös "Dinner, Lunch" -reseptit.
     *
     * record = Javan lyhyt tapa tehdä pelkkä tietopaketti (kentät, konstruktori ja getterit syntyvät itse).
     */
    public record MealSlot(String mealType, double share, String recipeCategory) {
    }

    // Reseptikannan kategoriat. Kaikki välipalat haetaan Snack-kategoriasta.
    // Iltapala (Supper) haetaan aamupalareseptien joukosta (päätetty 7.11.2026).
    private static final String CATEGORY_BREAKFAST = "Breakfast";
    private static final String CATEGORY_LUNCH = "Lunch";
    private static final String CATEGORY_DINNER = "Dinner";
    private static final String CATEGORY_SNACK = "Snack";

    /**
     * Ateriamäärä -> päivän ateriat. Jokaisella rivillä osuuksien summa on 1.0 (= 100 %).
     * 4 ateriaa = perinteinen suomalainen ateriarytmi: aamupala, lounas, päivällinen ja iltapala.
     * Välipalat tulevat mukaan vasta, kun aterioita on vähintään 5.
     */
    private static final Map<Integer, List<MealSlot>> SLOTS_BY_COUNT = Map.of(
            1, List.of(
                    dinner(1.00)),
            2, List.of(
                    lunch(0.45), dinner(0.55)),
            3, List.of(
                    breakfast(0.30), lunch(0.35), dinner(0.35)),
            4, List.of(
                    breakfast(0.25), lunch(0.30), dinner(0.30), supper(0.15)),
            5, List.of(
                    breakfast(0.25), lunch(0.30), snack(SNACK, 0.10), dinner(0.25), supper(0.10)),
            6, List.of(
                    breakfast(0.20), snack(MORNING_SNACK, 0.10), lunch(0.25), snack(SNACK, 0.10),
                    dinner(0.25), supper(0.10)),
            7, List.of(
                    breakfast(0.20), snack(MORNING_SNACK, 0.10), lunch(0.20), snack(SNACK, 0.10),
                    dinner(0.20), snack(EVENING_SNACK, 0.10), supper(0.10)),
            8, List.of(
                    breakfast(0.15), snack(MORNING_SNACK, 0.10), lunch(0.20), snack(SNACK, 0.10),
                    dinner(0.20), snack(EVENING_SNACK, 0.10), supper(0.10), snack(LATE_SNACK, 0.05)));

    // Konstruktori on private: luokasta ei tehdä olioita, vaan sitä käytetään staattisten metodien kautta.
    private MealSlots() {
    }

    /**
     * Palauttaa päivän ateriat profiilin ateriamäärän mukaan, päivän kulun mukaisessa järjestyksessä.
     * Jos ateriamäärää ei ole annettu (null) tai se on välin 1–8 ulkopuolella, käytetään 4:ää ateriaa.
     */
    public static List<MealSlot> forMealsPerDay(Integer mealsPerDay) {
        if (mealsPerDay == null || !SLOTS_BY_COUNT.containsKey(mealsPerDay)) {
            return SLOTS_BY_COUNT.get(DEFAULT_MEALS_PER_DAY);
        }
        return SLOTS_BY_COUNT.get(mealsPerDay);
    }

    /**
     * Onko nimi jokin sallituista aterioista? Käytetään tallennettaessa valintaa.
     * null-tarkistus ensin: List.of-listan contains(null) kaataisi ohjelman (NullPointerException),
     * jolloin puuttuva ateriatyyppi pyynnössä antaisi virheen 500 eikä 400.
     */
    public static boolean isKnownMealType(String mealType) {
        return mealType != null && ALL_MEAL_TYPES.contains(mealType);
    }

    // Apumetodit, jotta yllä oleva taulukko pysyy lyhyenä ja luettavana.
    private static MealSlot breakfast(double share) {
        return new MealSlot(BREAKFAST, share, CATEGORY_BREAKFAST);
    }

    private static MealSlot lunch(double share) {
        return new MealSlot(LUNCH, share, CATEGORY_LUNCH);
    }

    private static MealSlot dinner(double share) {
        return new MealSlot(DINNER, share, CATEGORY_DINNER);
    }

    private static MealSlot supper(double share) {
        return new MealSlot(SUPPER, share, CATEGORY_BREAKFAST);
    }

    private static MealSlot snack(String name, double share) {
        return new MealSlot(name, share, CATEGORY_SNACK);
    }
}
