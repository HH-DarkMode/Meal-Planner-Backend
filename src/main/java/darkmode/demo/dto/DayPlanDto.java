package darkmode.demo.dto;

import java.util.List;

/**
 * Paivan ateriaehdotukset: jokaiselle aterialle useita vaihtoehtoja.
 * Minka tahansa aamiaisen, lounaan ja paivallisen yhdistelma osuu suurin
 * piirtein paivatavoitteeseen.
 */
public record DayPlanDto(
        List<RecipeSummaryDto> breakfast,
        List<RecipeSummaryDto> lunch,
        List<RecipeSummaryDto> dinner
) {
}
