# Build stage
FROM maven:3.8.5-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN MAVEN_OPTS="-Xmx256m" mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
RUN mkdir -p /app/data && chmod 777 /app/data
EXPOSE 8080
ENTRYPOINT ["java", "-Xmx384m", "-jar", "app.jar"]
