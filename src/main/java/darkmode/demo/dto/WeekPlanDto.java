package darkmode.demo.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Viikkonäkymän vastaus.
 *
 * @param start     viikon ensimmäinen päivä (maanantai)
 * @param mealTypes käyttäjän päivän ateriat järjestyksessä (esim. Breakfast, Lunch, Dinner, Supper).
 *                  Frontend piirtää jokaiselle päivälle nämä paikat, myös tyhjät.
 * @param entries   viikon rivit (ehdotukset ja hyväksytyt) päivän ja aterian mukaan järjestettynä
 */
public record WeekPlanDto(LocalDate start, List<String> mealTypes, List<MealPlanEntryDto> entries) {
}
