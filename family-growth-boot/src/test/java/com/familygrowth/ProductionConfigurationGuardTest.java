package com.familygrowth;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ProductionConfigurationGuardTest {
    @Test
    void rejectsEmptyPasswordNonPostgresAndDisabledTls() {
        MockEnvironment emptyPassword = validEnvironment();
        emptyPassword.setProperty("spring.datasource.password", "");
        assertThatThrownBy(() -> new ProductionConfigurationGuard(emptyPassword).afterPropertiesSet())
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("spring.datasource.password");

        MockEnvironment nonPostgres = validEnvironment();
        nonPostgres.setProperty("spring.datasource.url", "jdbc:h2:mem:unsafe");
        assertThatThrownBy(() -> new ProductionConfigurationGuard(nonPostgres).afterPropertiesSet())
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("PostgreSQL");

        MockEnvironment noTls = validEnvironment();
        noTls.setProperty("server.ssl.enabled", "false");
        assertThatThrownBy(() -> new ProductionConfigurationGuard(noTls).afterPropertiesSet())
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("TLS");
    }

    @Test
    void acceptsExplicitPostgresCredentialsAndFileKeystore() {
        assertThatCode(() -> new ProductionConfigurationGuard(validEnvironment()).afterPropertiesSet()).doesNotThrowAnyException();
    }

    private static MockEnvironment validEnvironment() {
        return new MockEnvironment()
            .withProperty("spring.datasource.url", "jdbc:postgresql://db:5432/family_growth")
            .withProperty("spring.datasource.username", "family_growth")
            .withProperty("spring.datasource.password", "nonblank-test-value")
            .withProperty("server.ssl.enabled", "true")
            .withProperty("server.ssl.key-store", "file:/run/secrets/family-growth.p12")
            .withProperty("server.ssl.key-store-password", "nonblank-test-value");
    }
}
