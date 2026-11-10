package com.example.derivatives.demo;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
@Profile("demo")
public class DemoDataController {
    private final DemoDataService demoDataService;

    public DemoDataController(DemoDataService demoDataService) {
        this.demoDataService = demoDataService;
    }

    @PostMapping("/reset")
    @PreAuthorize("hasAnyRole('trader', 'risk_manager', 'viewer')")
    DemoDataService.ResetResponse reset() {
        return demoDataService.reset();
    }

    @Configuration
    @Profile("demo")
    static class SeedConfiguration {
        @Bean
        ApplicationRunner seedDemoData(DemoDataService demoDataService) {
            return arguments -> demoDataService.reset();
        }
    }
}
