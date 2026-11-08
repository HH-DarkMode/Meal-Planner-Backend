-- Paivan ateriasuunnitelman rivit (MealPlanSelection-entiteetti,
-- paketti darkmode.demo.mealplan).
-- Sovellus kayttaa ddl-auto=validate, joten taulu pitaa luoda tasta tiedostosta:
--   psql -U mealplanner -d mealplanner -h localhost -f db/meal-plan-schema.sql
CREATE TABLE IF NOT EXISTS meal_plan_selections (
    id         BIGSERIAL PRIMARY KEY,
    profile_id BIGINT NOT NULL,
    plan_date  DATE NOT NULL,
    meal_type  VARCHAR(20) NOT NULL,
    recipe_id  INTEGER NOT NULL REFERENCES recipes(recipe_id),
    eaten      BOOLEAN NOT NULL DEFAULT FALSE,
    -- UUSI: rivin tila. SUGGESTED = jarjestelman ehdotus, jota kayttaja ei ole viela hyvaksynyt.
    -- ACCEPTED = kayttaja on hyvaksynyt aterian suunnitelmaansa (nakyy kalenterissa).
    status     VARCHAR(20) NOT NULL DEFAULT 'ACCEPTED',
    UNIQUE (profile_id, plan_date, meal_type)
);

-- Jo olemassa olevaan tauluun (ajetaan kerran, turvallista ajaa uudelleen)
ALTER TABLE meal_plan_selections
    ADD COLUMN IF NOT EXISTS eaten BOOLEAN NOT NULL DEFAULT FALSE;

-- UUSI: status-sarake jo olemassa olevaan tauluun (turvallista ajaa uudelleen).
-- DEFAULT 'ACCEPTED' tayttaa kaikille vanhoille riveille arvon ACCEPTED, koska ne ovat
-- kayttajan reseptisivulla hyvaksymia aterioita. Nain vanha data toimii kuten ennenkin.
ALTER TABLE meal_plan_selections
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACCEPTED';
