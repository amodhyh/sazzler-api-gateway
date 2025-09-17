###############################################################################
#. Base Image Selection
#jdk image is used as the base image for building and running a Spring Boot application.
#base image to serve as the foundation for the other build stages in
# this file.
#
#For a Spring Boot application, the base image should be a JDK, not Alpine Linux, because you need Java to build and run your app.
#FROM eclipse-temurin:17-jdk AS build is correct for the build stage.
#do not need a separate base stage with Alpine.

# By specifying the "latest" tag, it will also use whatever happens to be the
# most recent version of that image when you build your Dockerfile.
# If reproducibility is important, consider using a versioned tag
# (e.g., alpine:3.17.2) or SHA (e.g., alpine@sha256:c41ab5c992deb4fe7e5da09f67a8804a46bd0592bfdf0b1847dde0e0889d2bff).
FROM gradle:8.12.1-jdk21 AS build
WORKDIR /home/gradle/project

# copy root Gradle configuration so multi-project build works
COPY settings.gradle settings.gradle
COPY gradle gradle
COPY gradle.properties gradle.properties
COPY build.gradle build.gradle

# copy service sources
COPY Sazzler-API-Gateway/ Sazzler-API-Gateway/
WORKDIR /home/gradle/project/Sazzler-API-Gateway

# build (skip tests to speed up CI)
RUN gradle clean build --no-daemon -x test

################################################################################
# Create a final stage for running your application.
#3. Runtime stage of the application.
#
# The following commands copy the output from the "build" stage above and tell
# the container runtime to execute it when the image is run. Ideally this stage
# contains the minimal runtime dependencies for the application as to produce
# the smallest image possible. This often means using a different and smaller
# image than the one used for building the application, but for illustrative
#The runtime section uses a JRE image to run your built Spring Boot JAR. It sets the
#working directory, copies the JAR from the build stage, exposes the app port, and sets the entrypoint.
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app
#Copy the built JAR file from the build stage to the runtime stage.
#The --from=build flag specifies that the source of the copied file is from the "build" stage.
#The path /app/build/libs/*.jar assumes that the build process outputs the JAR file
# to the /app/build/libs/ directory in the build stage.
# Adjust the path as necessary to match your build output.
COPY --from=build /home/gradle/project/Sazzler-API-Gateway/build/libs/*.jar app.jar
# Expose the port that the application will run on.
# This is a documentation instruction and does not actually publish the port.
# The port number should match the one your application is configured to use.
EXPOSE 8080
# The ENTRYPOINT instruction specifies the command that will be run when a container
# is started from the image. Here, it runs the Java application using the java -jar
# command, pointing to the JAR file copied earlier.
ENTRYPOINT ["java", "-jar", "app.jar"]
# purposes, we're using the same image for both build and runtime.