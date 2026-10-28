FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /build/target/team-skeleton.jar app.jar
# CA bundle for verified SSL to RDS (referenced by sslrootcert in the JDBC URLs)
ADD --chmod=644 https://truststore.pki.rds.amazonaws.com/global/global-bundle.pem /app/rds-global-bundle.pem
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
