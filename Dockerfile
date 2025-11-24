# Docker 镜像构建
# @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
# @from <a href="https://www.ai4labos.com/">ai4labOS</a>
FROM maven:3.8.6-jdk-8-slim as builder

# Copy local code to the container image.
WORKDIR /app
COPY pom.xml .
COPY src ./src

# Build a release artifact.
RUN mvn package -DskipTests

# Run the web service on container startup.
# Profile is controlled via SPRING_PROFILES_ACTIVE environment variable
# Spring Boot will automatically load application.yml from /app/config/ directory (highest priority)
# If not found, it falls back to classpath:/application.yml
CMD ["java","-jar","/app/target/springboot-init-0.0.1-SNAPSHOT.jar"]