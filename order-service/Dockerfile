# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

COPY .mvn/ .mvn
COPY mvnw pom.xml ./

RUN chmod +x ./mvnw

COPY src ./src

RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw package -Dmaven.test.skip=true -B -q

FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 8080

CMD ["java", \
     "-XX:+UseContainerSupport", \
     "-XX:MaxRAMPercentage=75.0", \
     "-jar", "app.jar"]
