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
                "'ledger_entry','idempotency_operation','gift_money','exchange_rule','exchange_preview','exchange_order'," +
                "'reward_product','reward_order','saving_account','saving_transaction','wish','wish_allocation'," +
                "'virtual_fund','fund_nav','fund_fee_rule','fund_trade_preview','fund_position','fund_trade_order'," +
                "'usage_policy','usage_event','withdrawal_rule','withdrawal_quote','withdrawal_request','withdrawal_action'," +
                "'child_experience_profile','child_experience_audit','documentary_source','documentary_source_action'," +
                "'education_resource_source','education_resource_source_stage','education_resource_category'," +
                "'education_resource_action','teaching_course','teaching_course_version','teaching_unit','teaching_lesson'," +
                "'learning_activity','learning_question','learning_question_option','lesson_assignment','activity_attempt'," +
                "'learning_completion','mastery_evidence','teaching_action')",
            Integer.class);
        assertThat(successfulMigrations).isEqualTo(11);
        assertThat(productionTables).isEqualTo(55);
    }
}
