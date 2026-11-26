# ${artifactId}

Quickstart project for working with the **Tarantool CRUD API** via Spring Data Tarantool.

## Requirements

- Java 17+
- Docker (for integration tests)

## Run the application

```bash
mvn clean package
java -jar target/${artifactId}-${version}.jar
```

The application connects to Tarantool at localhost:3301 (see src/main/resources/application.yml).

## Run the tests

```bash
mvn test
```

The tests use TarantoolCartridgeContainer, which starts a Tarantool  Cartridge cluster with the crud and vshard modules preinstalled.

## Important for production

- The CrudRepository layer is backed by Lua stored procedures from the  crud module (crud.insert, crud.select, etc.). Therefore the target  Tarantool must be a Cartridge cluster with the crud-router and  crud-storage roles enabled, not a standalone instance.
- Manage the schema via migrations, not by creating spaces in application code.

## Dependencies

- `io.tarantool:tarantool-spring-data-33` — CRUD via tarantool/crud
- `io.tarantool:testcontainers-java-tarantool` — test cluster
