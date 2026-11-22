-- Päivän ateriasuunnitelman rivit (MealPlanSelection-entiteetti,
-- paketti darkmode.demo.mealplan).
-- Sovellus käyttää ddl-auto=validate, joten taulu pitää luoda tästä tiedostosta:
--   psql -U mealplanner -d mealplanner -h localhost -f db/meal-plan-schema.sql
CREATE TABLE IF NOT EXISTS meal_plan_selections (
    id         BIGSERIAL PRIMARY KEY,
    profile_id BIGINT NOT NULL,
    plan_date  DATE NOT NULL,
    meal_type  VARCHAR(20) NOT NULL,
    recipe_id  INTEGER NOT NULL REFERENCES recipes(recipe_id),
    eaten      BOOLEAN NOT NULL DEFAULT FALSE,
    -- Rivin tila. SUGGESTED = järjestelmän ehdotus, jota käyttäjä ei ole vielä hyväksynyt.
    -- ACCEPTED = käyttäjä on hyväksynyt aterian suunnitelmaansa (näkyy kalenterissa).
    status     VARCHAR(20) NOT NULL DEFAULT 'ACCEPTED',
    UNIQUE (profile_id, plan_date, meal_type)
);

-- Jo olemassa olevaan tauluun (ajetaan kerran, turvallista ajaa uudelleen)
ALTER TABLE meal_plan_selections
    ADD COLUMN IF NOT EXISTS eaten BOOLEAN NOT NULL DEFAULT FALSE;

-- Status-sarake jo olemassa olevaan tauluun (turvallista ajaa uudelleen).
-- DEFAULT 'ACCEPTED' täyttää kaikille vanhoille riveille arvon ACCEPTED, koska ne ovat
-- käyttäjän hyväksymiä aterioita. Näin vanha data toimii kuten ennenkin.
ALTER TABLE meal_plan_selections
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACCEPTED';
