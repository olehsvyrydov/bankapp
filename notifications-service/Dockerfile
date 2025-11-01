FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY target/notifications-service-1.0.0.jar app.jar

EXPOSE 8087

ENTRYPOINT ["java", "-jar", "app.jar"]
