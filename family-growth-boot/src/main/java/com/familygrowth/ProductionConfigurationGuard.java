package com.familygrowth;

import java.net.URI;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public final class ProductionConfigurationGuard implements InitializingBean {
    private final Environment environment;

    public ProductionConfigurationGuard(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void afterPropertiesSet() {
        String databaseUrl = required("spring.datasource.url");
        if (!databaseUrl.startsWith("jdbc:postgresql://")) {
            throw new IllegalStateException("Production requires PostgreSQL");
        }
        required("spring.datasource.username");
        required("spring.datasource.password");
        if (!environment.getProperty("server.ssl.enabled", Boolean.class, false)) {
            throw new IllegalStateException("Production TLS must be enabled");
        }
        String keyStore = required("server.ssl.key-store");
        required("server.ssl.key-store-password");
        URI keyStoreUri = URI.create(keyStore);
        if (!("file".equalsIgnoreCase(keyStoreUri.getScheme()) || "classpath".equalsIgnoreCase(keyStoreUri.getScheme()))) {
            throw new IllegalStateException("Production TLS keystore must use file: or classpath:");
        }
    }

    private String required(String key) {
        String value = environment.getProperty(key);
        if (value == null || value.isBlank()) throw new IllegalStateException("Missing required production property: " + key);
        return value;
    }
}
