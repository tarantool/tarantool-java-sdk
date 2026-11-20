# ${artifactId}

Quickstart project for working with the **Tarantool BOX API** via the Tarantool Java SDK.

## Requirements

- Java 17+
- A running Tarantool instance on `localhost:3301`

## Run

```bash
mvn clean package
java -jar target/${artifactId}-${version}.jar
```

## What the example does

1. Connects to Tarantool via `TarantoolFactory.box()`.
2. Idempotently creates the person space and the primary pk index.
3. Inserts the tuple `[1, 'Alice']`.
4. Reads the tuple by key 1 and prints it to the log.

## Important for production
Creating the schema via eval on application startup is an anti-pattern.
In a real project, manage the schema via migrations:

- Flyway with Java migrations
- tarantool/migrations (Lua)
- or an external schema deployment tool

`if_not_exists = true` in the example only exists so that re-running the
quickstart does not fail with "Space 'person' already exists".

## Dependencies

- `io.tarantool:tarantool-client:1.7.3` — Box API
- Logback + SLF4J — logging
