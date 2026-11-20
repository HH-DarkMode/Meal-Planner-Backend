-- Käyttäjän suosikkireseptit (FavoriteRecipe-entiteetti, paketti darkmode.demo.favorite).
-- Sovellus käyttää ddl-auto=validate, joten taulu pitää luoda tästä tiedostosta.
-- Aja vasta, kun recipes- ja profiles-taulut ovat olemassa (taulu viittaa niihin):
--   psql -U mealplanner -d mealplanner -h localhost -f db/favorite-schema.sql
-- Turvallista ajaa uudelleen (IF NOT EXISTS).
CREATE TABLE IF NOT EXISTS favorite_recipes (
    id         BIGSERIAL PRIMARY KEY,
    -- Kenen suosikki. Jos profiili poistetaan, sen suosikit poistuvat samalla (ON DELETE CASCADE).
    profile_id BIGINT NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    -- Mikä resepti. Viittaus varmistaa, ettei suosikkeihin voi tallentaa olematonta reseptiä.
    recipe_id  INTEGER NOT NULL REFERENCES recipes(recipe_id),
    -- Milloin lisätty. My recipes -sivu näyttää uusimman ensin.
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Sama resepti voi olla saman käyttäjän suosikeissa vain kerran.
    UNIQUE (profile_id, recipe_id)
);
