# ---- Build stage -----------------------------------------------------------
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
COPY library-gateway/pom.xml library-gateway/pom.xml
COPY library-gateway/src library-gateway/src

RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp -f library-gateway/pom.xml -Dmaven.test.skip=true package

# ---- Runtime stage ---------------------------------------------------------
FROM eclipse-temurin:21-jre-jammy

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 10001 app \
    && useradd --system --uid 10001 --gid app --home-dir /app --shell /usr/sbin/nologin app

WORKDIR /app
COPY --from=build /workspace/library-gateway/target/library-gateway-1.0.0.jar /app/app.jar

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

USER app
EXPOSE 8081

HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=6 \
    CMD curl -fsS http://localhost:8081/actuator/health/readiness || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
