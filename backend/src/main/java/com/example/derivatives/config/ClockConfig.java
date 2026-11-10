package com.example.derivatives.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

@Configuration
public class ClockConfig {

    @Bean
    @Profile("demo")
    Clock demoClock(@Value("${demo.fixed-instant}") Instant fixedInstant) {
        return Clock.fixed(fixedInstant, ZoneOffset.UTC);
    }

    @Bean
    @Profile("!demo")
    Clock systemClock() {
        return Clock.systemUTC();
    }
}
