FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY target/accounts-service-1.0.0.jar app.jar

EXPOSE 8081

ENV SPRING_PROFILES_ACTIVE=docker

ENTRYPOINT ["java", "-jar", "app.jar"]
