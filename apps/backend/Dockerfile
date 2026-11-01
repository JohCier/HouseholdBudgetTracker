# syntax=docker/dockerfile:1
# Build context: repository root (the build needs the parent pom.xml and the Maven wrapper there)

# Java version: same major version as java.version in the parent pom.xml and .java-version (CI); Dependabot
# updates the image tags, raising the major version means changing all three.

# --- Build: Spring-Boot-JAR bauen und in Schichten zerlegen -------------------
FROM eclipse-temurin:25-jdk AS build
WORKDIR /build
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY apps/backend/pom.xml apps/backend/
# Maven runs in the service directory and finds the parent via relativePath; from the root it would also need the
# pom.xml of the other service (module list of the aggregator).
WORKDIR /build/apps/backend
RUN --mount=type=cache,target=/root/.m2 ../../mvnw -B dependency:go-offline
COPY apps/backend/src/ src/
# Tests laufen in der CI vor dem Image-Build (inkl. Testcontainers, das hier kein Docker hätte). maven.test.skip
# übersetzt sie auch nicht, deshalb braucht das Image die gemeinsamen Test-Hilfen aus libs/test-support nicht.
RUN --mount=type=cache,target=/root/.m2 ../../mvnw -B package -Dmaven.test.skip=true \
    && java -Djarmode=tools -jar target/*.jar extract --layers --launcher --destination /build/extracted

# --- Runtime -----------------------------------------------------------------
FROM eclipse-temurin:25-jre
RUN useradd --system --no-create-home --uid 10001 app
WORKDIR /app
# Eine Schicht pro Änderungshäufigkeit: Abhängigkeiten ändern sich selten, der eigene Code oft.
COPY --from=build /build/extracted/dependencies/ ./
COPY --from=build /build/extracted/spring-boot-loader/ ./
COPY --from=build /build/extracted/snapshot-dependencies/ ./
COPY --from=build /build/extracted/application/ ./
USER app
EXPOSE 8080
# Weder curl noch wget sind im Image; Bash kann HTTP über /dev/tcp sprechen.
HEALTHCHECK --interval=15s --timeout=3s --start-period=30s --retries=3 \
    CMD ["bash", "-c", "exec 3<>/dev/tcp/127.0.0.1/8080 && printf 'GET /actuator/health/readiness HTTP/1.0\\r\\n\\r\\n' >&3 && grep -q '\"UP\"' <&3"]
# Datenbank über SPRING_DATASOURCE_URL / _USERNAME / _PASSWORD, Token-Secret über JWT_SECRET,
# JVM-Optionen über JAVA_TOOL_OPTIONS, z. B. -e JAVA_TOOL_OPTIONS="-Xmx256m"
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
