# Stage 1: Build the application using Maven
FROM maven:3.9.4-eclipse-temurin-17 AS build

# Set the working directory inside the container
WORKDIR /app

# Copy the pom.xml and download dependencies
COPY pom.xml ./
RUN mvn dependency:go-offline

# Copy the rest of the application source code
COPY src ./src

# Package the application (run Maven package inside the container)
RUN mvn clean package

# Stage 2: Create a lightweight image to run the Spring Boot application
FROM eclipse-temurin:17-jdk-jammy

# Set the working directory inside the container
WORKDIR /app

# Copy the JAR file from the first stage (Maven build) to this stage
COPY --from=build /app/target/videodownloader-0.0.1-SNAPSHOT.jar /app/videodownloader.jar

# Expose the port the app runs on
EXPOSE 8080

# Run the Spring Boot application
ENTRYPOINT ["java", "-jar", "/app/videodownloader.jar"]

