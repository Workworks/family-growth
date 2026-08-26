FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace
COPY . .
RUN chmod +x mvnw && ./mvnw -pl family-growth-boot -am -DskipTests package

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S familygrowth && adduser -S -G familygrowth familygrowth
WORKDIR /app
COPY --from=build /workspace/family-growth-boot/target/family-growth-boot-*.jar app.jar
USER familygrowth
EXPOSE 8443
ENTRYPOINT ["java", "-jar", "/app/app.jar", "--spring.profiles.active=prod"]
