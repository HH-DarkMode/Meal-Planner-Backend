-- Profiilitaulut (Profile-entiteetti, paketti darkmode.demo.profile).
-- Sovellus kayttaa ddl-auto=validate, joten taulut pitaa luoda tasta tiedostosta:
--   psql -U mealplanner -d mealplanner -h localhost -f db/profile-schema.sql
CREATE TABLE IF NOT EXISTS profiles (
    id        BIGSERIAL PRIMARY KEY,
    username  VARCHAR(255),
    weight    INTEGER,
    height    INTEGER,
    gender    VARCHAR(255),
    goal      VARCHAR(255),
    calories  INTEGER,
    protein   INTEGER,
    fat       INTEGER,
    carbs     INTEGER
);

CREATE TABLE IF NOT EXISTS profile_diets (
    profile_id  BIGINT NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    diets       VARCHAR(255)
);
