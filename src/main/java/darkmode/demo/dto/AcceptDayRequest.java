package darkmode.demo.dto;

import java.time.LocalDate;

/** Accept all: hyväksy päivän kaikki ehdotukset. Esim. { "date": "2026-11-09" } */
public record AcceptDayRequest(LocalDate date) {
}
