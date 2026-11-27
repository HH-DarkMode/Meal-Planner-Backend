package darkmode.demo.service;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/** Ruokavalioiden sanalistat. Esimerkit ovat oikeita tunnistamattomia ainesosia reseptikannasta. */
class DietIngredientRulesTest {

    /** Vegaanilta ja kasvissyöjältä estetään liha ja kala, myös jäänteet kuten "skinless". */
    @Test
    void meatAndFishAreBlockedForVeganAndVegetarian() {
        for (String diet : List.of("vegan", "vegetarian")) {
            assertThat(blocks(diet, "skinless")).isTrue();
            assertThat(blocks(diet, "italian sausage")).isTrue();
            assertThat(blocks(diet, "worcestershire sauce")).isTrue();
            assertThat(blocks(diet, "smoked salmon")).isTrue();
            assertThat(blocks(diet, "clams")).isTrue(); // monikko
        }
    }

    /** Lihan jäänteet ja reseptien nimet: "round" (ground round), "chops", "Spaghetti and Meatballs". */
    @Test
    void meatCutsAndMeatRecipeNamesAreBlocked() {
        assertThat(blocks("vegan", "round")).isTrue();
        assertThat(blocks("vegan", "chops")).isTrue();
        assertThat(blocks("vegan", "bone-in")).isTrue();
        assertThat(blocks("vegan", "Jenn's Out Of This World Spaghetti and Meatballs")).isTrue();
        assertThat(blocks("vegan", "Vegan Chicken Nuggets")).isFalse();
        assertThat(blocks("vegan", "Roasted Cauliflower Steaks")).isFalse();
    }

    /** Sallitut sanat ja kokonaiset sanat: ei osumia väärään paikkaan. */
    @Test
    void plantBasedNamesAreNotBlocked() {
        assertThat(blocks("vegan", "vegan chicken strips")).isFalse();  // "vegan" sallii
        assertThat(blocks("vegan", "eggplant")).isFalse();
        assertThat(blocks("vegan", "graham cracker crumbs")).isFalse(); // "ham" ei osu
        assertThat(blocks("vegan", "oyster mushrooms")).isFalse();
        assertThat(blocks("vegan", "greek yogurt")).isFalse();          // vegaanille vinkki, ei esto
    }

    /** Maidottomalta estetään maitotuotteet, mutta kasvimaidot ja -voit sallitaan. */
    @Test
    void dairyIsBlockedForDairyFreeButPlantMilksAreAllowed() {
        assertThat(blocks("dairy-free", "greek yogurt")).isTrue();
        assertThat(blocks("dairy-free", "milk chocolate chips")).isTrue();
        assertThat(blocks("dairy-free", "unsweetened almond milk")).isFalse();
        assertThat(blocks("dairy-free", "creamy peanut butter")).isFalse();
        assertThat(blocks("dairy-free", "cream of tartar")).isFalse();
    }

    /** Muut ruokavaliot eivät estä mitään sanalistalla. */
    @Test
    void otherDietsHaveNoBlockedWords() {
        assertThat(DietIngredientRules.blockedPattern("gluten-free")).isNull();
        assertThat(DietIngredientRules.blockedPattern("high-protein")).isNull();
    }

    /** Vegaanin korvausvinkki: maitotuotteet, kananmuna, hunaja ja liivate, ei kasvimaitoja. */
    @Test
    void veganSwapsAreDairyEggsHoneyAndGelatin() {
        assertThat(DietIngredientRules.isVeganSwap("butter")).isTrue();
        assertThat(DietIngredientRules.isVeganSwap("eggs")).isTrue();
        assertThat(DietIngredientRules.isVeganSwap("unflavored gelatin")).isTrue();
        assertThat(DietIngredientRules.isVeganSwap("half-and-half cream")).isTrue();
        assertThat(DietIngredientRules.isVeganSwap("coconut milk")).isFalse();
        assertThat(DietIngredientRules.isVeganSwap("egg substitute")).isFalse();
    }

    /** Mausteet ovat turvallisia, mutta eivät jos nimessä on myös eläinperäinen sana. */
    @Test
    void spicesAreSafeUnlessTheyContainAnimalWords() {
        assertThat(DietIngredientRules.isSafeForAllDiets("vanilla extract")).isTrue();
        assertThat(DietIngredientRules.isSafeForAllDiets("and black pepper")).isTrue();
        assertThat(DietIngredientRules.isSafeForAllDiets("cayenne pepper")).isTrue();
        assertThat(DietIngredientRules.isSafeForAllDiets("pepper jack cheese")).isFalse();
        assertThat(DietIngredientRules.isSafeForAllDiets("taco seasoning mix")).isFalse();
    }

    /** Sama tarkistus kuin tietokantahaussa: estetty sana eikä sallittua sanaa. */
    private static boolean blocks(String diet, String ingredient) {
        String blocked = DietIngredientRules.blockedPattern(diet);
        return blocked != null
                && Pattern.compile(blocked, Pattern.CASE_INSENSITIVE).matcher(ingredient).find()
                && !Pattern.compile(DietIngredientRules.allowedPattern(), Pattern.CASE_INSENSITIVE).matcher(ingredient).find();
    }
}
