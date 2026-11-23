# syntax=docker/dockerfile:1.7
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -pl msvc-gateway -am package -Dmaven.test.skip=true

FROM eclipse-temurin:21-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd -r -u 10001 appuser \
    && mkdir -p /app/logs /app/secrets \
    && chown -R appuser:appuser /app
WORKDIR /app
COPY --from=build --chown=appuser:appuser /app/msvc-gateway/target/msvc-gateway-*.jar /app/app.jar
USER appuser
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -Duser.timezone=UTC"
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
