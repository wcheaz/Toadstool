# Build stage
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
COPY Main.java .

RUN mvn -q -DskipTests package

# Runtime stage
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/target/toadstool-1.0.0.jar app.jar

CMD ["java", "-jar", "app.jar"]
