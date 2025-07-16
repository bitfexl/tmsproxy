FROM maven:3-eclipse-temurin-21 AS build-app
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline
COPY . .
RUN mvn package
RUN mv /build/target/tmsproxy.jar /app.jar

# https://github.com/docker-library/docs/blob/master/eclipse-temurin/README.md#creating-a-jre-using-jlink
FROM eclipse-temurin:21 AS build-jre
WORKDIR /build
COPY --from=build-app /app.jar .
# jdk.crypto.ec module for ssl https://stackoverflow.com/a/55517159/30915739
RUN $JAVA_HOME/bin/jlink \
         --add-modules jdk.crypto.ec,$($JAVA_HOME/bin/jdeps --ignore-missing-deps --print-module-deps app.jar) \
         --strip-debug \
         --no-man-pages \
         --no-header-files \
         --compress=2 \
         --output /javaruntime

FROM debian:12-slim
WORKDIR /
COPY --from=build-jre /javaruntime /javaruntime
COPY --from=build-app /app.jar .
RUN mkdir /config
COPY docker-default-tmsconfig.json /config/tmsconfig.json
CMD ["/javaruntime/bin/java", "-jar", "/app.jar", "/config/tmsconfig.json"]