package com.example.elibrary.shared.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {

    /**
     * All business timestamps come from this Clock. Tests can replace the bean with a fixed clock.
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
