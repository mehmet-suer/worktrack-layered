package com.worktrack.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.AuditorAware;

import java.util.Optional;

@Configuration
public class TestAuditorAwareConfig {

    @Bean
    @Primary
    public AuditorAware<String> auditorAware() {
        return () -> Optional.of("test-auditor");
    }
}