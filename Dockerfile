# syntax=docker/dockerfile:1

# --- Frontend: Angular-App bauen ---------------------------------------------
FROM node:22-alpine AS frontend
WORKDIR /build/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci
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
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B package -DskipTests \
    && cp target/*.jar /build/app.jar

# --- Runtime -----------------------------------------------------------------
FROM eclipse-temurin:25-jre
RUN useradd --system --no-create-home --uid 10001 app
WORKDIR /app
COPY --from=backend /build/app.jar app.jar
USER app
EXPOSE 8080
# JVM-Optionen zur Laufzeit über JAVA_TOOL_OPTIONS setzen, z. B. -e JAVA_TOOL_OPTIONS="-Xmx256m"
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
