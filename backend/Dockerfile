FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY . .
RUN mvn -B -f pom.xml package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
ARG APP_MODULE
ARG APP_ARTIFACT
COPY --from=build "/workspace/${APP_MODULE}/target/${APP_ARTIFACT}-1.0.0-SNAPSHOT.jar" app.jar
USER 10001
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
