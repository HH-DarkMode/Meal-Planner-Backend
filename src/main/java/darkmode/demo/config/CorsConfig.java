package darkmode.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sallii frontendin kutsua kaikkia /api-endpointteja suoraan.
 *
 * Frontend ja backend ajetaan eri osoitteissa, ja selain estää oletuksena kutsut
 * toiseen origin:iin (CORS). Sallitut originit luetaan asetuksesta app.cors.allowed-origins
 * (pilkuilla eroteltu lista), jotta julkaisuympäristön frontend-osoite voidaan asettaa
 * ympäristömuuttujalla ilman koodimuutosta (ks. application-rahti.properties).
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final String[] allowedOrigins;

    public CorsConfig(@Value("${app.cors.allowed-origins}") String allowedOrigins) {
        this.allowedOrigins = allowedOrigins.split("\\s*,\\s*");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
    }
}
