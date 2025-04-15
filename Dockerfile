FROM maven:3.9.9-amazoncorretto-21-debian

# add debian packages
WORKDIR /tmp
RUN apt update && apt upgrade -y && apt install -y wget
RUN wget https://github.com/jgm/pandoc/releases/download/3.6.3/pandoc-3.6.3-1-amd64.deb
RUN dpkg -i pandoc-3.6.3-1-amd64.deb

# install new ris certificate
RUN wget https://repo.harica.gr/certs/HARICA-TLS-Root-2021-RSA.pem
RUN keytool -import -trustcacerts -file HARICA-TLS-Root-2021-RSA.pem -alias HARICA-TLS-RSA -keystore /usr/lib/jvm/java-21-amazon-corretto/bin/cacerts -storepass changeit -noprompt

RUN addgroup --system spring && adduser --system spring --home /user/spring && adduser spring spring
USER spring:spring

WORKDIR /app

# first, only resolve dependencies, so that we can cache them until a pom.xml change happens
COPY --chown=spring:spring pom.xml .
RUN mvn -B dependency:resolve


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