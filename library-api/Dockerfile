# ---- Build stage -----------------------------------------------------------
# The Maven build is isolated from the runtime image so the shipped image has
# no compiler, source tree or local Maven repository.
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
COPY library-api/pom.xml library-api/pom.xml
COPY library-api/src library-api/src

RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp -f library-api/pom.xml -Dmaven.test.skip=true package

# ---- Runtime stage ---------------------------------------------------------
FROM eclipse-temurin:21-jre-jammy

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 10001 app \
    && useradd --system --uid 10001 --gid app --home-dir /app --shell /usr/sbin/nologin app \
    && mkdir -p /app/data \
    && chown -R app:app /app

WORKDIR /app
COPY --from=build /workspace/library-api/target/e-library-service-1.0.0.jar /app/app.jar

ENV ELIBRARY_DB_PATH=/app/data/library.db \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

USER app
EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=3s --start-period=40s --retries=6 \
    CMD curl -fsS http://localhost:8080/actuator/health/readiness || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
