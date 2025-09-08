package com.worktrack.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "testcontainers.mysql")
public record TestContainersProperties(
        String image,
        String database,
        String username,
        String password
) { }
