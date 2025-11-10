FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY target/auth-server-1.0.0.jar app.jar

EXPOSE 9100

ENV SPRING_PROFILES_ACTIVE=docker

ENTRYPOINT ["java", "-jar", "app.jar"]
