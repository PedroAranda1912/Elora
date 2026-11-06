package com.elora.marketplace.config;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!h2")
public class DatabaseConfiguration {

    @Bean
    DataSource dataSource(DataSourceProperties properties) {
        PostgresDatabaseUrl url = PostgresDatabaseUrl.parse(properties.getUrl());
        properties.setUrl(url.jdbcUrl());
        if (url.username() != null) {
            properties.setUsername(url.username());
        }
        if (url.password() != null) {
            properties.setPassword(url.password());
        }
        return properties.initializeDataSourceBuilder().build();
    }
}
