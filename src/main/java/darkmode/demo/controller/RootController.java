package darkmode.demo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Juuripolun vastaus, jotta osoitteeseen meneminen ilman endpointtia ei näytä virheeltä. */
@RestController
public class RootController {

    @GetMapping("/")
    public Map<String, String> root() {
        return Map.of(
                "status", "ok",
                "message", "AI Meal Planner backend is running",
                "docs", "/swagger-ui/index.html"
        );
    }
}
