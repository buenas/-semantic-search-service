# Base image (Java runtime)
FROM eclipse-temurin:17-jdk

#Working directory inside container
WORKDIR /app

#Copy the built jar into container
COPY target/*.jar app.jar

#Expose app port
EXPOSE 8080

#Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]