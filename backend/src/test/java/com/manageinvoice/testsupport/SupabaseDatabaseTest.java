package com.manageinvoice.testsupport;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ActiveProfiles("test")
@ContextConfiguration(initializers = SupabaseDatabaseTest.IsolateSchema.class)
public @interface SupabaseDatabaseTest {
    class IsolateSchema implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext context) {
            var environment = context.getEnvironment();
            Map<String, Object> properties = new LinkedHashMap<>();
            properties.put("spring.datasource.hikari.connection-init-sql", "SET search_path TO ledgerly_test");
            properties.put("spring.jpa.properties.hibernate.default_schema", "ledgerly_test");
            properties.put("spring.jpa.hibernate.ddl-auto", "validate");
            properties.put("spring.sql.init.mode", "never");
            properties.put("spring.flyway.enabled", "true");
            properties.put("spring.flyway.schemas", "ledgerly_test");
            properties.put("spring.flyway.default-schema", "ledgerly_test");
            properties.put("spring.flyway.create-schemas", "true");
            properties.put("spring.flyway.clean-disabled", "true");
            properties.put("spring.flyway.url", environment.getRequiredProperty("spring.datasource.url"));
            properties.put("spring.flyway.user", environment.getRequiredProperty("spring.datasource.username"));
            properties.put("spring.flyway.password", environment.getRequiredProperty("spring.datasource.password"));
            environment.getPropertySources().addFirst(new MapPropertySource("isolatedSupabaseTest", properties));
        }
    }
}
