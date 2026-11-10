FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /workspace/backend
COPY backend/pom.xml .
RUN mvn -B dependency:go-offline
COPY backend/src ./src
RUN mvn -B package

FROM eclipse-temurin:21-jre-alpine AS runtime
RUN addgroup -S demo && adduser -S demo -G demo
WORKDIR /app
COPY --from=build /workspace/backend/target/derivatives-demo-0.1.0-SNAPSHOT.jar app.jar
USER demo
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=20s --retries=6 \
  CMD wget -q -O - http://127.0.0.1:8080/actuator/health | grep -q '"status":"UP"'
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
