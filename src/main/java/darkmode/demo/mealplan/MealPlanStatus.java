package darkmode.demo.mealplan;

/**
 * Ateriasuunnitelman rivin tila.
 *
 * enum = Javan tapa määritellä kiinteä joukko sallittuja arvoja. Muuttujaan
 * voi asettaa vain nämä kaksi arvoa, joten kirjoitusvirhe (esim. "ACEPTED")
 * huomataan jo käännösvaiheessa eikä vasta tietokannassa.
 */
public enum MealPlanStatus {

    /**
     * Järjestelmän tekemä ateriaehdotus. Näkyy Meal plan -sivun viikkonäkymässä,
     * mutta EI kalenterissa eikä makrojen yhteenvedoissa. Syödyksi ei voi merkitä.
     */
    SUGGESTED,

    /**
     * Käyttäjän hyväksymä ateria. Näkyy kalenterissa suunnitelmana,
     * ja sen voi merkitä syödyksi.
     */
    ACCEPTED
}
