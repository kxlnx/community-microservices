FROM eclipse-temurin:21-jre
WORKDIR /app
ARG MODULE
COPY ${MODULE}/target/${MODULE}-*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
