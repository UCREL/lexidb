FROM openjdk:12-ea-jdk-alpine AS build

RUN apk add --no-cache wget unzip

WORKDIR /opt

RUN wget -O gradle.zip "https://services.gradle.org/distributions/gradle-5.2-all.zip" \
    && unzip -d ./gradle ./gradle.zip \
    && rm ./gradle.zip 
ENV PATH=$PATH:/opt/gradle/gradle-5.2/bin

WORKDIR /lexidb

COPY src /lexidb/src/
COPY gradle /lexidb/gradle/
COPY build.gradle /lexidb/
COPY gradlew /lexidb/
COPY gradlew.bat /lexidb/
COPY lombok.config /lexidb/
COPY settings.gradle /lexidb/

RUN gradle build



FROM openjdk:16-alpine

RUN addgroup --system java_user && adduser -S -s /bin/false -G java_user java_user
RUN mkdir /lexidb \
    && chown -R java_user:java_user /lexidb

COPY --from=build --chown=java_user:java_user /lexidb/build/libs/lexidb-2.0.jar /lexidb/
COPY --from=build --chown=java_user:java_user /lexidb/src/main/resources/app.properties /lexidb/

WORKDIR /lexidb

EXPOSE 1189
USER java_user

ENTRYPOINT ["java", "-jar", "lexidb-2.0.jar"]
CMD ["./app.properties"]

