# syntax=docker/dockerfile:1

# Node major version; CI passes the value from frontend/.nvmrc.
ARG NODE_VERSION=22

# --- Frontend: Angular-App bauen ---------------------------------------------
FROM node:${NODE_VERSION}-alpine AS frontend
WORKDIR /build/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci
COPY api/openapi.json /build/api/openapi.json
COPY frontend/ ./
RUN npm run build

# --- Backend: Spring-Boot-JAR inkl. Frontend unter classpath:/static bauen ---
FROM eclipse-temurin:25-jdk AS backend
WORKDIR /build/backend
COPY backend/.mvn/ .mvn/
COPY backend/mvnw backend/pom.xml ./
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B dependency:go-offline
COPY backend/src/ src/
COPY --from=frontend /build/frontend/dist/frontend/browser/ src/main/resources/static/
# Tests laufen in der CI vor dem Image-Build (inkl. Testcontainers, das hier kein Docker hätte).
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B package -DskipTests \
    && java -Djarmode=tools -jar target/*.jar extract --layers --launcher --destination /build/extracted

# --- Runtime -----------------------------------------------------------------
FROM eclipse-temurin:25-jre
RUN useradd --system --no-create-home --uid 10001 app
WORKDIR /app
# Eine Schicht pro Änderungshäufigkeit: Abhängigkeiten ändern sich selten, der eigene Code oft.
COPY --from=backend /build/extracted/dependencies/ ./
COPY --from=backend /build/extracted/spring-boot-loader/ ./
COPY --from=backend /build/extracted/snapshot-dependencies/ ./
COPY --from=backend /build/extracted/application/ ./
USER app
EXPOSE 8080
# Weder curl noch wget sind im Image; Bash kann HTTP über /dev/tcp sprechen.
HEALTHCHECK --interval=15s --timeout=3s --start-period=30s --retries=3 \
    CMD ["bash", "-c", "exec 3<>/dev/tcp/127.0.0.1/8080 && printf 'GET /actuator/health/readiness HTTP/1.0\\r\\n\\r\\n' >&3 && grep -q '\"UP\"' <&3"]
# Datenbank über SPRING_DATASOURCE_URL / _USERNAME / _PASSWORD,
# JVM-Optionen über JAVA_TOOL_OPTIONS, z. B. -e JAVA_TOOL_OPTIONS="-Xmx256m"
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
