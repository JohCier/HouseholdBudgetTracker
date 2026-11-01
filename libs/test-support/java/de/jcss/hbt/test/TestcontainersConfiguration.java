package de.jcss.hbt.test;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Provides a throwaway PostgreSQL, shared by all tests that use the same Spring context. */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    private static final Pattern POSTGRES_IMAGE = Pattern.compile("(?m)^\\s+image:\\s*[\"']?(postgres:[^\\s\"']+)");

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(postgresImage());
    }

    /**
     * The Postgres image of compose.yaml in the repository root, so that tests run against the same version as the
     * local stack and production, and Dependabot (ecosystem docker-compose) updates it in one place.
     */
    static String postgresImage() {
        Path composeFile = composeFile();
        try {
            Matcher matcher = POSTGRES_IMAGE.matcher(Files.readString(composeFile));
            if (!matcher.find()) {
                throw new IllegalStateException("No postgres image in " + composeFile);
            }
            return matcher.group(1);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Tests run in the service directory (apps/backend, apps/users), so compose.yaml lies a few levels up. */
    private static Path composeFile() {
        for (Path dir = Path.of("").toAbsolutePath(); dir != null; dir = dir.getParent()) {
            Path candidate = dir.resolve("compose.yaml");
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("compose.yaml not found above " + Path.of("").toAbsolutePath());
    }
}
