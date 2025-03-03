FROM maven:3.9.9-amazoncorretto-21-debian

RUN addgroup --system spring && adduser --system spring --home /user/spring && adduser spring spring
USER spring:spring

WORKDIR /app

# first, only resolve dependencies, so that we can cache them until a pom.xml change happens
COPY --chown=spring:spring pom.xml .
RUN mvn -B dependency:resolve


RUN ls -lahh

# copy source code
COPY --chown=spring:spring src ./src
COPY --chown=spring:spring config ./config

RUN mvn -B clean package -DskipTests

# Set the default active profile
ENV SPRING_PROFILES_ACTIVE=test

# Expose the port on which the app runs
EXPOSE 8080

# Run the app
ENTRYPOINT ["java","-jar","target/API-0.0.1-SNAPSHOT.jar"]