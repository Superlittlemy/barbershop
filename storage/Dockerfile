# Stage 1: Build
FROM maven:3.9-eclipse-temurin-11 AS builder

WORKDIR /app

# 先拷贝所有 pom（含父子 pom）以及子模块的 pom，让依赖解析走缓存
COPY pom.xml ./
COPY docker/settings.xml /root/.m2/settings.xml
COPY common/pom.xml ./common/
COPY storage/pom.xml ./storage/
COPY core/pom.xml ./core/

RUN mvn dependency:go-offline -B -s /root/.m2/settings.xml \
    -Dmaven.wagon.http.retryHandler.count=5 \
    -Dmaven.wagon.http.httpconnectionManager.ttlSeconds=120 \
    -Dmaven.wagon.http.readTimeout=60000 \
    -Dmaven.wagon.http.connectTimeout=30000

# 再拷贝源码
COPY common ./common
COPY storage ./storage

# 仅构建 storage 模块；-am 会自动构建其依赖（common）
RUN mvn -pl storage -am package -DskipTests -B -s /root/.m2/settings.xml

# Stage 2: Run
FROM eclipse-temurin:11-jre

WORKDIR /app

COPY --from=builder /app/storage/target/*.jar /app/app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
