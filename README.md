# Shrinkwrap.legal API

Backend for the shrinkwrap.legal extension - offering summaries of court judgements directly embedded into Austria's legal information System ("RIS")

## Prerequisites

- JDK 21
- Maven 3.9+ (or the Maven Wrapper, if present)
- PostgreSQL (if running the app locally without containers)
- Docker and Docker Compose (optional; if you prefer running via [compose.yaml](compose.yaml) which also provisions PostgreSQL)
- Internet access for dependency resolution and SOAP client code generation (the build, as configured in the pom.xml, generates sources from a remote WSDL)

## Build

The Maven build (see pom.xml) targets Java 21 and produces an executable Spring Boot fat JAR. 
It also generates SOAP client sources during the build lifecycle.

- Build the project:
```bash
mvn clean
```

- Run the application:
```bash
mvn spring-boot:run
```

### Alternative: Run with Docker Compose (includes PostgreSQL)

The provided compose.yaml starts both the API and a PostgreSQL database for you.

- Start the stack from the project root:
```bash
docker compose up --build
# or detach:
docker compose up -d
```

- Default bindings:
  - API: http://127.0.0.1:8080
  - PostgreSQL: 127.0.0.1:5432

- Stop and remove containers:
```bash
docker compose down
```

Note:
- The build generates SOAP client stubs from a public WSDL; if that endpoint is unreachable, the build may fail. Re-run once connectivity is restored.
- The WSDL file uses a certificate from the HARICA CA - please take a look at the [Dockerfile](Dockerfile) on how to add it to the Java keystore

## Configuration

Before running in a non-default setup, set your API keys in [application.properties](./src/main/resources/application.properties).

- spring.ai.openai.api-key — set to your OpenAI API key if you plan to use the OpenAI integration.

If you run against PostgreSQL without containers, configure your datasource properties (URL, username, password) accordingly in your `application.properties`.

## License

This project is licensed under the [GNU Affero General Public License v3.0](LICENSE).  

This project received financial support from [netidee](https://www.netidee.at/shrinkwraplegal).