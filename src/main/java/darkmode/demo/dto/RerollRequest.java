package darkmode.demo.dto;

import java.time.LocalDate;

/** Vaihda-nappi: uusi ehdotus yhteen ateriaan. today tulee selaimesta (oikea aikavyöhyke). */
public record RerollRequest(LocalDate date, String mealType, LocalDate today) {
}
