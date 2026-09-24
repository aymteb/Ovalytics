# syntax=docker/dockerfile:1
# Railway: Root Directory VIDE, Dockerfile path = backend/Dockerfile
# Contexte Docker = racine du repo

FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY backend/mvnw .
COPY backend/.mvn .mvn
COPY backend/pom.xml .
COPY backend/src src
RUN chmod +x mvnw && ./mvnw -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN apt-get update \
	&& apt-get install -y --no-install-recommends python3 \
	&& rm -rf /var/lib/apt/lists/*
COPY --from=build /app/target/*.jar app.jar
COPY backend/scripts /app/scripts
RUN mkdir -p /import /app/data/import
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
