package darkmode.demo.dto;

import java.time.LocalDate;

/** Pyyntö merkitä tietyn päivän ateria syödyksi tai syömättömäksi. */
public record SetEatenRequest(LocalDate date, String mealType, boolean eaten) {
}
