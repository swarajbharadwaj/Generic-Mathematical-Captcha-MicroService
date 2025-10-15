# --- Stage 1: Use a specific Java version for a consistent build environment ---
# We use the Eclipse Temurin distribution of Java 17, which is a standard choice.
FROM eclipse-temurin:21-jdk-jammy as builder

# Set the working directory inside the container
WORKDIR /workspace/app

# Copy the Maven project definition and source code
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src

# Build the application using Maven inside the container
# This ensures the build is clean and not dependent on your local machine's setup.
RUN ./mvnw install -DskipTests

# --- Stage 2: Create a slim, secure final image for running the app ---
# Using a JRE (Java Runtime Environment) image is smaller and more secure than a full JDK.
FROM eclipse-temurin:21-jre-jammy

# Define an argument for the JAR file path
ARG DEPENDENCY=/workspace/app/target

# Copy the built JAR file from the 'builder' stage into the final image
COPY --from=builder ${DEPENDENCY}/captcha-service-0.0.1-SNAPSHOT.jar app.jar

# Expose port 5000, which is what our Spring Boot app listens on
EXPOSE 5000

# The command that will run when the container starts
# This launches the Java application.
ENTRYPOINT ["java","-jar","app.jar"]
