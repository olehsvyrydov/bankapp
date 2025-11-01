FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY target/exchange-generator-service-1.0.0.jar app.jar

EXPOSE 8085

ENTRYPOINT ["java", "-jar", "app.jar"]
