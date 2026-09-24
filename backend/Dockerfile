# syntax=docker/dockerfile:1
# Railway: Root Directory = backend , Dockerfile = Dockerfile

FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src
RUN chmod +x mvnw && ./mvnw -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN apt-get update \
	&& apt-get install -y --no-install-recommends python3 \
	&& rm -rf /var/lib/apt/lists/*
COPY --from=build /app/target/*.jar app.jar
COPY scripts /app/scripts
RUN mkdir -p /import /app/data/import
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
