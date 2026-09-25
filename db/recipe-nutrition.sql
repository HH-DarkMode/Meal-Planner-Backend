-- Reseptien ravintoarvot PER ANNOS hakua varten (recipes-taulun valmiit sarakkeet).
-- Sääntö, mistä arvot otetaan:
--   1. 'source': lähteen (AllRecipes) oma arvio, jos kaikki neljä arvoa (kalorit, proteiini,
--      rasva, hiilihydraatit) löytyvät. Ne ovat valmiiksi per annos ja koko reseptin kattavia.
--   2. 'fineli': muuten meidän Fineli-datasta laskema arvo, mutta vain jos vähintään 80 %
--      ainesosariveistä on mukana laskennassa. Alle sen laskettu arvo jäisi liian pieneksi.
--   3. Muuten arvot jäävät tyhjiksi (NULL), eikä resepti tule ravintoarvohaun tuloksiin.
-- Tiedosto voi ajaa uudelleen turvallisesti (nollaa ensin arvot).
--   psql -U mealplanner -d mealplanner -h localhost -f db/recipe-nutrition.sql
ALTER TABLE recipes ADD COLUMN IF NOT EXISTS kcal_per_serving    DOUBLE PRECISION;
ALTER TABLE recipes ADD COLUMN IF NOT EXISTS protein_per_serving DOUBLE PRECISION;
ALTER TABLE recipes ADD COLUMN IF NOT EXISTS fat_per_serving     DOUBLE PRECISION;
ALTER TABLE recipes ADD COLUMN IF NOT EXISTS carbs_per_serving   DOUBLE PRECISION;
ALTER TABLE recipes ADD COLUMN IF NOT EXISTS nutrition_source    VARCHAR(10);

UPDATE recipes SET kcal_per_serving = NULL, protein_per_serving = NULL,
                   fat_per_serving = NULL, carbs_per_serving = NULL, nutrition_source = NULL;

UPDATE recipes
SET kcal_per_serving = src_calories_kcal, protein_per_serving = src_protein,
    fat_per_serving = src_fat, carbs_per_serving = src_carbs, nutrition_source = 'source'
WHERE src_calories_kcal IS NOT NULL AND src_protein IS NOT NULL
  AND src_fat IS NOT NULL AND src_carbs IS NOT NULL;

UPDATE recipes
SET kcal_per_serving = calc_energia_kcal / servings, protein_per_serving = calc_proteiini / servings,
    fat_per_serving = calc_rasva / servings, carbs_per_serving = calc_hiilihydraatti / servings,
    nutrition_source = 'fineli'
WHERE nutrition_source IS NULL
  AND servings > 0 AND calc_energia_kcal > 0
  AND total_lines > 0 AND matched_lines >= total_lines * 0.8;
