-- Paivan ateriasuunnitelmaan hyvaksytyt reseptit (MealPlanSelection-entiteetti,
-- paketti darkmode.demo.mealplan).
-- Sovellus kayttaa ddl-auto=validate, joten taulu pitaa luoda tasta tiedostosta:
--   psql -U mealplanner -d mealplanner -h localhost -f db/meal-plan-schema.sql
CREATE TABLE IF NOT EXISTS meal_plan_selections (
    id         BIGSERIAL PRIMARY KEY,
    profile_id BIGINT NOT NULL,
    plan_date  DATE NOT NULL,
    meal_type  VARCHAR(20) NOT NULL,
    recipe_id  INTEGER NOT NULL REFERENCES recipes(recipe_id),
    UNIQUE (profile_id, plan_date, meal_type)
);
