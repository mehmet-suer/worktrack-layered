package com.worktrack.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;


@TestConfiguration(proxyBeanMethods = false)
@EnableConfigurationProperties(TestContainersProperties.class)
public class TestContainersConfig {


    private final TestContainersProperties props;

    public TestContainersConfig(TestContainersProperties props) {
        this.props = props;
    }


    @Bean
    @ServiceConnection
    public MySQLContainer<?> mysqlContainer() {
        return new MySQLContainer<>(props.image())
                .withDatabaseName(props.database())
                .withUsername(props.username())
                .withPassword(props.password())
                .withReuse(true);
    }

}