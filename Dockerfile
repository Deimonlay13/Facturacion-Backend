# ---- Build ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Cachea dependencias
COPY pom.xml .
RUN mvn -q dependency:go-offline -B || true
# Compila y empaqueta
COPY src ./src
RUN mvn -q -B -DskipTests clean package

# ---- Run ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/facturacion-backend-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
