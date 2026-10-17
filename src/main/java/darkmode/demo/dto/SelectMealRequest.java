package darkmode.demo.dto;

/** Pyyntö hyväksyä resepti tietylle ateriatyypille tämän päivän ateriasuunnitelmaan. */
public record SelectMealRequest(String mealType, Integer recipeId) {
}
