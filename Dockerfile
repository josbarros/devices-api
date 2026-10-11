FROM docker.io/library/eclipse-temurin:21-jdk-jammy AS build

WORKDIR /workspace
COPY gradlew build.gradle settings.gradle ./
COPY gradle ./gradle
COPY src/main ./src/main
RUN ./gradlew --no-daemon bootJar

FROM docker.io/library/eclipse-temurin:21-jre-jammy

RUN groupadd --system devices \
    && useradd --system --gid devices devices \
    && mkdir -p /app /data \
    && chown devices:devices /data
WORKDIR /app
COPY --from=build /workspace/build/libs/devices-api-*.jar app.jar

USER devices:devices
ENV DEVICES_DATABASE_PATH=/data/devices.db
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
