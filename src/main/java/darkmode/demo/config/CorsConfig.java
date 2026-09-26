package darkmode.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sallii frontendin (Vite-kehityspalvelin) kutsua kaikkia /api-endpointteja suoraan.
 *
 * Frontend ajetaan eri portissa (5173) kuin backend (8080), ja selain estää oletuksena
 * kutsut toiseen porttiin (CORS). Frontend kutsuu backendiä osoitteella
 * http://localhost:8080 ilman välityspalvelinta, joten backendin pitää erikseen sallia
 * frontendin osoite. Portti 4173 on Viten esikatselu (npm run preview).
 *
 * Profiili-controllerissa on oma @CrossOrigin, joka sallii samat osoitteet. Tämä asetus
 * kattaa lisäksi reseptiendpointit.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5173", "http://localhost:4173")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
    }
}
