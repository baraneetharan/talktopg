# ---------- Stage 1: Build ----------
    FROM maven:3.9.14-eclipse-temurin-25 AS builder
    WORKDIR /app
   
    # Copy pom and download dependencies first (for caching)
    COPY pom.xml .
    RUN mvn dependency:go-offline
   
    # Copy source code
    COPY src ./src
   
    # Build the application
    RUN mvn clean package -DskipTests
   
    # ---------- Stage 2: Run ----------
    FROM eclipse-temurin:25-jdk
    WORKDIR /app
   
    # Copy jar from builder stage
    COPY --from=builder /app/target/*.jar app.jar
   
    # Expose default Spring Boot port
    EXPOSE 8080
   
    # Run the application
    ENTRYPOINT ["java", "-jar", "app.jar"]
 