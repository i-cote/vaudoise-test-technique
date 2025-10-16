FROM eclipse-temurin:17-jdk

# Update package list and install required tools
RUN apt-get update && \
    apt-get install -y \
    maven \
    vim \
    git \
    netcat-openbsd && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Expose port 8089 (configured in application.yml)
EXPOSE 8089

# Run the Spring Boot application
CMD ["java", "-jar", "target/clients-contracts-api-1.0.0.jar"]
