package com.familygrowth;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@EnabledIfEnvironmentVariable(named = "FAMILY_GROWTH_TEST_POSTGRES_URL", matches = ".+")
@SpringBootTest(properties = {
    "spring.datasource.url=${FAMILY_GROWTH_TEST_POSTGRES_URL}",
    "spring.datasource.username=${FAMILY_GROWTH_TEST_POSTGRES_USER:family_growth}",
    "spring.datasource.password=${FAMILY_GROWTH_TEST_POSTGRES_PASSWORD:}",
    "spring.jpa.hibernate.ddl-auto=validate"
})
class PostgresSchemaValidationTest {
    @Autowired JdbcTemplate jdbc;

    @Test
    void flywayMigratesAndHibernateValidatesOnPostgres() {
        Integer successfulMigrations = jdbc.queryForObject(
            "select count(*) from flyway_schema_history where success = true", Integer.class);
        Integer productionTables = jdbc.queryForObject(
            "select count(*) from information_schema.tables where table_schema = 'public' " +
                "and table_name in ('family','parent_profile','child_profile','growth_plan','growth_goal','growth_task'," +
                "'parent_pin_credential','auth_session','child_progress','wallet','task_completion'," +
                "'ledger_entry','idempotency_operation','gift_money','exchange_rule','exchange_preview','exchange_order')",
            Integer.class);
        assertThat(successfulMigrations).isEqualTo(4);
        assertThat(productionTables).isEqualTo(17);
    }
}
