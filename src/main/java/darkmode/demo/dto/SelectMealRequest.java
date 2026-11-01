package darkmode.demo.dto;

import java.time.LocalDate;

/**
 * Pyyntö hyväksyä resepti tietylle ateriatyypille ateriasuunnitelmaan.
 * date on valinnainen: jos sitä ei anneta, käytetään tätä päivää.
 */
public record SelectMealRequest(String mealType, Integer recipeId, LocalDate date) {
}
