package com.familygrowth;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@EnabledIfEnvironmentVariable(named = "STAGE2_POSTGRES_URL", matches = ".+")
@SpringBootTest(properties = {
    "spring.datasource.url=${STAGE2_POSTGRES_URL}",
    "spring.datasource.username=${STAGE2_POSTGRES_USER}",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=validate"
})
class PostgresSchemaValidationTest {
    @Autowired JdbcTemplate jdbc;

    @Test
    void flywayMigratesAndHibernateValidatesOnPostgres() {
        Integer successfulMigrations = jdbc.queryForObject(
            "select count(*) from flyway_schema_history where success = true", Integer.class);
        Integer coreTables = jdbc.queryForObject(
            "select count(*) from information_schema.tables where table_schema = 'public' " +
                "and table_name in ('family','parent_profile','child_profile','growth_plan','growth_goal','growth_task')",
            Integer.class);
        assertThat(successfulMigrations).isEqualTo(1);
        assertThat(coreTables).isEqualTo(6);
    }
}
