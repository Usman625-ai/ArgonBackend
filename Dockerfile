# --- Build stage: has full JDK + Maven, compiles the jar ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Copy pom.xml first and download dependencies separately — this layer is
# cached by Docker as long as pom.xml doesn't change, so code-only changes
# (the common case) skip re-downloading the entire dependency tree.
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# --- Runtime stage: only the JRE + the built jar, nothing else ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# Render sets $PORT at runtime; application.properties already reads it
# via server.port=${PORT:${SERVER_PORT:8080}}
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]