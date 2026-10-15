# syntax=docker/dockerfile:1
# Build context: apps/backend

# --- Build: Spring-Boot-JAR bauen und in Schichten zerlegen -------------------
FROM eclipse-temurin:25-jdk AS build
WORKDIR /build
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B dependency:go-offline
COPY src/ src/
# Tests laufen in der CI vor dem Image-Build (inkl. Testcontainers, das hier kein Docker hätte).
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B package -DskipTests \
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
