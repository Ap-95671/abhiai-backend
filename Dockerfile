FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline

COPY src src
RUN ./mvnw package -DskipTests

FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache curl tesseract-ocr tesseract-ocr-data-eng \
    && addgroup --system spring \
    && adduser --system --ingroup spring spring

WORKDIR /app
COPY --from=build --chown=spring:spring /workspace/target/*.jar app.jar
RUN mkdir -p /app/uploads && chown spring:spring /app/uploads

USER spring

ENV PORT=8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC"

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD curl --fail --silent "http://localhost:${PORT:-8080}/actuator/health" || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
