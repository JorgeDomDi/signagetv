package com.signagetv;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.TestPropertySource;

/**
 * Smoke test que arranca el contexto contra una BD H2 en memoria.
 * Excluye el SeedDataRunner (perfil dev) y Flyway no se ejecuta porque
 * deshabilitamos Flyway y dejamos a Hibernate crear el schema.
 */
@SpringBootTest(classes = SignageTvApplicationTests.TestApp.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:signagetv;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false",
        "app.jwt.secret=test-secret-test-secret-test-secret-test-secret",
        "app.storage.path=./build/test-storage",
        "spring.profiles.active=test"
})
class SignageTvApplicationTests {

    @SpringBootApplication
    @ComponentScan(
            basePackages = "com.signagetv",
            excludeFilters = @ComponentScan.Filter(
                    type = FilterType.REGEX,
                    pattern = "com\\.signagetv\\.config\\.SeedDataRunner")
    )
    static class TestApp { }

    @Test
    void contextLoads() {
        // si el contexto carga, el smoke test pasa
    }
}
