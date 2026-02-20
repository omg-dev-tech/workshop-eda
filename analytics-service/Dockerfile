# syntax=docker/dockerfile:1.6

# ---- Build stage ------------------------------------------------------------
FROM gradle:8.8-jdk21-alpine AS build
WORKDIR /workspace

# 캐시 최적화: 의존성 먼저 다운로드
COPY build.gradle settings.gradle gradle.properties ./
COPY gradle gradle
RUN --mount=type=cache,target=/home/gradle/.gradle \
    gradle --no-daemon build -x test || true

# 실제 소스 복사 후 빌드
COPY src src
RUN --mount=type=cache,target=/home/gradle/.gradle \
    gradle --no-daemon clean bootJar

# ---- Runtime stage ----------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
ENV TZ=Asia/Seoul \
    LANG=C.UTF-8 \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseContainerSupport"

# 만들어진 fat jar 하나를 app.jar로 복사
# (gradle bootJar 결과가 1개라고 가정)
COPY --from=build /workspace/build/libs/*.jar /app/app.jar

EXPOSE 8084
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]