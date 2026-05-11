# ===== Stage 1: Build =====
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build

# 先拷依赖描述以利用 docker 层缓存
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw mvnw
RUN chmod +x mvnw && mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests \
 && mv target/*.jar /build/app.jar

# ===== Stage 2: Runtime =====
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 文档存储目录（应用代码里默认 ./data/documents）
RUN mkdir -p /app/data/documents

COPY --from=builder /build/app.jar /app/app.jar

ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -Dfile.encoding=UTF-8"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
