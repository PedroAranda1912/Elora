package com.elora.marketplace.config;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PostgresDatabaseUrlTest {

    @Test
    void convertsAivenUriAndDecodesEmbeddedCredentials() {
        PostgresDatabaseUrl result = PostgresDatabaseUrl.parse(
                "postgresql://avnadmin:p%40ss%2Bword@pg.example.test:12345/defaultdb?sslmode=require");

        assertThat(result.jdbcUrl()).isEqualTo(
                "jdbc:postgresql://pg.example.test:12345/defaultdb?sslmode=require");
        assertThat(result.username()).isEqualTo("avnadmin");
        assertThat(result.password()).isEqualTo("p@ss+word");
    }

    @Test
    void preservesExistingJdbcUrlsAndTheirSeparateCredentials() {
        PostgresDatabaseUrl result = PostgresDatabaseUrl.parse(
                "jdbc:postgresql://localhost:5432/elora?sslmode=require");

        assertThat(result.jdbcUrl()).isEqualTo("jdbc:postgresql://localhost:5432/elora?sslmode=require");
        assertThat(result.username()).isNull();
        assertThat(result.password()).isNull();
    }

    @Test
    void buildsDataSourceFromAivenUri() {
        DataSourceProperties properties = new DataSourceProperties();
        properties.setUrl("postgres://avnadmin:p%40ss%2Bword@pg.example.test:12345/defaultdb?sslmode=require");

        try (HikariDataSource dataSource =
                     (HikariDataSource) new DatabaseConfiguration().dataSource(properties)) {
            assertThat(dataSource.getJdbcUrl()).isEqualTo(
                    "jdbc:postgresql://pg.example.test:12345/defaultdb?sslmode=require");
            assertThat(dataSource.getUsername()).isEqualTo("avnadmin");
            assertThat(dataSource.getPassword()).isEqualTo("p@ss+word");
        }
    }

    @Test
    void rejectsUnsupportedOrIncompleteUris() {
        assertThatThrownBy(() -> PostgresDatabaseUrl.parse("https://db.example.test/elora"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DB_URL");
        assertThatThrownBy(() -> PostgresDatabaseUrl.parse("postgres://avnadmin@db.example.test"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("database name");
    }
}
