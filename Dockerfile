FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY . .
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/stylehub-1.0.0.jar app.jar
COPY --from=build /app/target/lib lib
COPY --from=build /app/*.html ./
COPY --from=build /app/*.css ./
COPY --from=build /app/*.js ./
COPY --from=build /app/assets assets
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
