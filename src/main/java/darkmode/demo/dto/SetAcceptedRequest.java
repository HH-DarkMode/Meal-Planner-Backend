package darkmode.demo.dto;

import java.time.LocalDate;

/**
 * Hyväksy (accepted=true) tai peru hyväksyntä (accepted=false).
 * Esim. { "date": "2026-11-09", "mealType": "Lunch", "accepted": true }
 */
public record SetAcceptedRequest(LocalDate date, String mealType, boolean accepted) {
}
