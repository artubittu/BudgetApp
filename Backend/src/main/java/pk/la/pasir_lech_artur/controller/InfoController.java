package pk.la.pasir_lech_artur.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class InfoController {

    @GetMapping("/api/info")
    public Map<String, String> getInfo() {
        return Map.of(
                "appName", "Aplikacja Budżetowa",
                "version", "1.0",
                "message", "Witaj w aplikacji budżetowej stworzonej ze Spring Boot!"
        );
    }
}