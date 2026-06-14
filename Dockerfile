# Stage 1: Build
FROM maven:3.9-eclipse-temurin-11 AS builder

WORKDIR /app

COPY pom.xml .
COPY docker/settings.xml /root/.m2/settings.xml
RUN mvn dependency:go-offline -B -s /root/.m2/settings.xml \
    -Dmaven.wagon.http.retryHandler.count=5 \
    -Dmaven.wagon.httpconnectionManager.ttlSeconds=120 \
    -Dmaven.wagon.http.readTimeout=60000 \
    -Dmaven.wagon.http.connectTimeout=30000

COPY src ./src
RUN mvn package -DskipTests -B -s /root/.m2/settings.xml

# Stage 2: Run
FROM eclipse-temurin:11-jre

WORKDIR /app

COPY --from=builder /app/target/barbershop.jar ./barbershop.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "barbershop.jar"]