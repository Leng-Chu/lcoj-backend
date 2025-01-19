FROM openjdk:8-jdk-alpine

# Copy local code to the container image.
WORKDIR /app

COPY lcoj-backend-0.0.1-SNAPSHOT.jar /app/lcoj-backend-0.0.1-SNAPSHOT.jar

EXPOSE 8101

# Run the web service on container startup.
CMD ["java", "-jar", "/app/lcoj-backend-0.0.1-SNAPSHOT.jar","--spring.profiles.active=prod"]
