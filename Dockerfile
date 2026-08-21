FROM maven:3.9.16-amazoncorretto-25-debian

# add debian packages
WORKDIR /tmp
RUN apt update && apt upgrade -y && apt install -y wget
RUN wget https://github.com/jgm/pandoc/releases/download/3.6.3/pandoc-3.6.3-1-amd64.deb
RUN dpkg -i pandoc-3.6.3-1-amd64.deb

# install new ris certificate
RUN wget https://repo.harica.gr/certs/HARICA-TLS-Root-2021-RSA.pem
RUN keytool -import -trustcacerts -file HARICA-TLS-Root-2021-RSA.pem -alias HARICA-TLS-RSA -keystore /usr/lib/jvm/java-21-amazon-corretto/lib/security/cacerts -storepass changeit -noprompt
RUN wget https://repo.harica.gr/certs/HARICA-GEANT-TLS-R1.pem
RUN keytool -import -trustcacerts -file HARICA-GEANT-TLS-R1.pem -alias HARICA-GEANT-TLS-R1 -keystore /usr/lib/jvm/java-21-amazon-corretto/lib/security/cacerts -storepass changeit -noprompt

RUN addgroup --system spring && adduser --system spring --home /user/spring && adduser spring spring
USER spring:spring
RUN id spring

WORKDIR /app
# first, only resolve dependencies, so that we can cache them until a pom.xml change happens
COPY --chown=spring:spring pom.xml .
RUN --mount=type=cache,target=/user/spring/.m2,uid=100,gid=102 mvn -B dependency:go-offline


# copy source code
COPY --chown=spring:spring src ./src
COPY --chown=spring:spring config ./config

RUN --mount=type=cache,target=/user/spring/.m2,uid=100,gid=102 mvn -B clean package -DskipTests

# Set the default active profile
ENV SPRING_PROFILES_ACTIVE=prod

# Expose the port on which the app runs
EXPOSE 8080

# Run the app
ENTRYPOINT ["java","-jar","target/API-0.0.2-SNAPSHOT.jar"]
#ENTRYPOINT ["/bin/bash"]