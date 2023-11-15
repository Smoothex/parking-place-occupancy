# Build stage
FROM  gradle:8.4.0-jdk17 AS BUILD
WORKDIR /app
COPY backend-postgresql-api/ .
RUN gradle clean build

# Run stage
FROM openjdk:22-ea-17-slim
COPY --from=BUILD /app/build/libs/*.jar app.jar
EXPOSE 8080
# if you want to connect to locally running PostgreSQL, remove "-Dspring.profiles.active=docker"
CMD ["java", "-jar", "/app.jar", "-Dspring.profiles.active=docker"]
