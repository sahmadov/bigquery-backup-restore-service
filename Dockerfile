# Stage 1: Build the application
FROM eclipse-temurin:17-jdk as builder

# Set the working directory
WORKDIR /build

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN apt-get update && apt-get install -y maven
RUN mvn dependency:go-offline

# Copy source code and build
COPY src/ ./src/
RUN mvn package -DskipTests

# Stage 2: Create the runtime container
FROM eclipse-temurin:17-jdk

# Set the working directory
WORKDIR /app

# Copy only the built JAR from the builder stage
COPY --from=builder /build/target/bigquery-backup-restore-service.jar ./app.jar

# Expose Cloud Run port
EXPOSE 8080

# Set Cloud Run's default port as an environment variable
ENV PORT=8080

# Run the application
CMD ["java", "-jar", "app.jar"]