package de.jcss.hbt;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a test that starts the whole application against a throwaway PostgreSQL
 * (Testcontainers) and therefore needs Docker. Such tests are tagged {@code integration}:
 * {@code ./mvnw test} skips them, {@code ./mvnw verify} runs them afterwards via Failsafe.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Tag("integration")
public @interface IntegrationTest {
}
