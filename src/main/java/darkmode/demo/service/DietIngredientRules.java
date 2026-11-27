package darkmode.demo.service;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Ruokavalioiden sanalistat ainesosille. Fineli tunnistaa vain osan reseptien ainesosista,
 * joten tunnistamattomia ainesosia tarkistetaan näillä listoilla nimen perusteella.
 *
 * Sanat tunnistetaan kokonaisina sanoina, ja monikko (s-loppu) hyväksytään: "egg" löytää myös
 * "eggs", mutta ei sanaa "eggplant". Samoin "ham" ei löydä sanaa "graham".
 * Kuvio toimii sekä Javassa että PostgreSQL:ssä, joten samaa kuviota käytetään tietokantahaussa.
 */
public final class DietIngredientRules {

    // ---- eläinperäiset ainesosat ryhmittäin ----

    /**
     * Liha ja siipikarja. Mukana myös jäänteet ja leikkausten nimet, joihin datan siivous on lyhentänyt
     * rivejä: "skinless"/"boneless"/"bone-in" (esim. "boneless skinless chicken breast"), "chops"
     * (pork chops) ja "round" (ground round = naudan jauheliha). "steak" puuttuu tarkoituksella,
     * koska esim. "cauliflower steak" on kasvisruoka.
     */
    static final List<String> MEAT = List.of("chicken", "beef", "pork", "bacon", "ham", "turkey", "sausage",
            "lamb", "veal", "meat", "meatball", "chorizo", "pepperoni", "kielbasa", "andouille", "salami",
            "prosciutto", "skinless", "boneless", "bone-in", "chops", "round", "chuck", "sirloin", "loin",
            "tenderloin", "brisket", "drumstick", "venison", "duck", "goose", "liver", "lard");

    /** Kala ja äyriäiset. Worcestershire-kastikkeessa on anjovista. */
    static final List<String> FISH = List.of("fish", "salmon", "tuna", "shrimp", "prawn", "crab", "lobster",
            "clam", "oyster", "scallop", "mussel", "anchovy", "anchovies", "sardine", "cod", "worcestershire",
            "roe", "caviar");

    /** Maitotuotteet. */
    static final List<String> DAIRY = List.of("milk", "butter", "cheese", "cream", "yogurt", "buttermilk",
            "half-and-half", "ghee", "whey", "mozzarella", "parmesan", "ricotta", "mascarpone");

    /** Kananmuna ja muut eläinperäiset, joille on helppo kasviperäinen korvike. */
    static final List<String> EGG_AND_OTHER = List.of("egg", "mayonnaise", "honey", "gelatin");

    /**
     * Sallitut: kasviperäiset tuotteet, vaikka nimessä on eläinperäinen sana
     * (esim. "almond milk" sisältää sanan "milk", mutta on vegaaninen).
     */
    static final List<String> ALLOWED = List.of("vegan", "plant-based", "plant based", "meatless", "meat-free",
            "dairy-free", "non-dairy", "almond milk", "soy milk", "oat milk", "rice milk", "coconut milk",
            "cashew milk", "peanut butter", "almond butter", "cashew butter", "coconut butter", "cocoa butter",
            "apple butter", "butter or margarine", "butter flavored shortening", "coconut cream",
            "cream of coconut", "cream of tartar", "cream-style corn", "egg substitute", "egg replacer",
            "butter beans", "oyster mushroom");

    /** Mausteet, yrtit ja muut aina kasviperäiset ainesosat: näistä ei tule ⚠-varoitusta. */
    static final List<String> SAFE_FOR_ALL_DIETS = List.of("pepper", "peppercorn", "salt", "cumin", "nutmeg",
            "cayenne", "paprika", "chili powder", "red pepper flakes", "cinnamon", "turmeric", "curry powder",
            "oregano", "thyme", "rosemary", "basil", "parsley", "cilantro", "dill", "sage", "bay leaf", "leaf",
            "leaves", "ginger", "garlic powder", "onion powder", "italian seasoning", "pumpkin pie spice",
            "allspice", "clove", "coriander", "extract", "cooking spray", "spray", "baking soda",
            "baking powder", "water", "vegetable broth", "vegetable bouillon", "vegetable stock");

    // Valmiiksi käännetyt kuviot Java-tarkistuksia varten (CASE_INSENSITIVE = isot ja pienet kirjaimet samoin).
    private static final Pattern ANY_ANIMAL = pattern(concat(MEAT, FISH, DAIRY, EGG_AND_OTHER));
    private static final Pattern VEGAN_SWAP = pattern(concat(DAIRY, EGG_AND_OTHER));
    private static final Pattern ALLOWED_WORDS = pattern(ALLOWED);
    private static final Pattern SAFE = pattern(SAFE_FOR_ALL_DIETS);

    private DietIngredientRules() {
    }

    /**
     * Tietokantahaun kuvio: sanat, jotka estävät reseptin ruokavaliolta, kun ainesosaa ei ole tunnistettu.
     * Vegaani ja kasvissyöjä: liha ja kala. Vegaanille maitotuotteet ja kananmuna eivät estä, vaan niistä
     * näytetään korvausvinkki. Maidoton: maitotuotteet. Muut ruokavaliot: null (ei estoa).
     */
    public static String blockedPattern(String diet) {
        return switch (diet) {
            case "vegan", "vegetarian" -> wordPattern(concat(MEAT, FISH));
            case "dairy-free" -> wordPattern(DAIRY);
            default -> null;
        };
    }

    /** Tietokantahaun kuvio sallituille sanoille (ks. ALLOWED). */
    public static String allowedPattern() {
        return wordPattern(ALLOWED);
    }

    /** Onko ainesosa aina kasviperäinen (esim. mauste)? Silloin siitä ei tarvita varoitusta. */
    public static boolean isSafeForAllDiets(String ingredient) {
        return SAFE.matcher(ingredient).find() && !ANY_ANIMAL.matcher(ingredient).find();
    }

    /** Onko ainesosa maitotuote, kananmuna, hunaja tai liivate, jolle vegaani voi käyttää korviketta? */
    public static boolean isVeganSwap(String ingredient) {
        return VEGAN_SWAP.matcher(ingredient).find() && !ALLOWED_WORDS.matcher(ingredient).find();
    }

    // ---- apumetodit ----

    /** Kokonaiset sanat, monikko sallittu: (^|ei kirjain)(sana1|sana2)(s)?(ei kirjain|$) */
    private static String wordPattern(List<String> words) {
        return "(^|[^a-z])(" + String.join("|", words) + ")s?([^a-z]|$)";
    }

    private static Pattern pattern(List<String> words) {
        return Pattern.compile(wordPattern(words), Pattern.CASE_INSENSITIVE);
    }

    @SafeVarargs
    private static List<String> concat(List<String>... lists) {
        return Stream.of(lists).flatMap(List::stream).toList();
    }
}
