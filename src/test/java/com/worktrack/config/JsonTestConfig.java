package com.worktrack.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.worktrack.util.JsonUtils;
import org.springframework.context.annotation.Bean;

public class JsonTestConfig {

    @Bean
    JsonUtils jsonUtils(ObjectMapper mapper) {
        return new JsonUtils(mapper);
    }
}
