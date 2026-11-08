package darkmode.demo.dto;

import java.time.LocalDate;

/**
 * Yksi ateriasuunnitelman rivi: päivä, ateriatyyppi, syöty-tila, resepti ja tila.
 * status = "SUGGESTED" (ehdotus) tai "ACCEPTED" (hyväksytty).
 *
 * record on Javan lyhyt tapa tehdä pelkkä tietopaketti: Java luo itse kentät,
 * konstruktorin ja hakumetodit (date(), eaten() jne.). Arvoja ei voi muuttaa
 * luomisen jälkeen. Backend lähettää tämän frontille JSON:ina.
 */

public record MealPlanEntryDto(
    LocalDate date,
    String mealType,
    boolean eaten,
    RecipeSummaryDto recipe,
    String status) {

}
