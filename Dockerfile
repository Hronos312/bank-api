FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
ARG MODULE
COPY pom.xml .
COPY services ./services
RUN --mount=type=cache,id=bank-api-maven-repo,target=/root/.m2/repository,sharing=locked \
    mvn -B -pl "services/${MODULE}" -am package -Dmaven.test.skip=true && \
    cp "services/${MODULE}"/target/*.jar /app/app.jar

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/app.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
