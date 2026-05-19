# Stage 1: Build
FROM maven:3.9-eclipse-temurin-11 AS builder

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -DskipTests -B

# Stage 2: Run
FROM eclipse-temurin:11-jre

WORKDIR /app

COPY --from=builder /app/target/barbershop.jar ./barbershop.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "barbershop.jar"]