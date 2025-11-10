FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY target/front-ui-1.0.0.jar app.jar

EXPOSE 8090

ENTRYPOINT ["java", "-jar", "app.jar"]
