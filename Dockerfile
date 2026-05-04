FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests


FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar
COPY src/main/resources/email.properties ./email.properties
COPY src/main/resources/sms.properties ./sms.properties
COPY src/main/resources/telegram.properties ./telegram.properties

EXPOSE 8082

ENTRYPOINT ["java", "-cp", "app.jar:.", "com.example.otp.Main"]