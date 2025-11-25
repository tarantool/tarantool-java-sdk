# Installation Guide

This guide provides instructions for installing and building the Tarantool Java SDK for different build systems.

## Prerequisites

- Java 8 or higher
- Maven 3.9.11 or higher (for Maven builds)
- Git (for cloning the repository)

## Installation Methods

### Maven

#### Using Public Repository

Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>io.tarantool</groupId>
    <artifactId>tarantool-client</artifactId>
    <version>1.5.0</version>
</dependency>
```

### Gradle

#### Using Public Repository

Add the following to your `build.gradle`:

```gradle
dependencies {
    implementation 'io.tarantool:tarantool-client:1.5.0'
}
```

Or for Gradle Kotlin DSL (`build.gradle.kts`):

```kotlin
dependencies {
    implementation("io.tarantool:tarantool-client:1.5.0")
}
```

### Building from Source

1. Clone the repository:
   ```bash
   git clone https://github.com/tarantool/tarantool-java-sdk.git
   cd tarantool-java-sdk
   ```

2. Build the project with Maven:
   ```bash
   # Full build with tests
   ./mvnw clean install
   
   # Build without running tests (faster)
   ./mvnw clean install -DskipTests
   
   # Build without tests and with minimal logging
   ./mvnw clean install -DskipTests -q
   ```

## Run Tests

This project uses JUnit 5 for testing and includes both unit tests and integration tests.
The project has different test profiles for different types of tests.

### Running All Tests

```bash
# Run all tests (unit + integration)
./mvnw test

# Run all tests during installation
./mvnw clean install
```

### Skipping Tests

#### Skip All Tests (Unit and Integration)

```bash
# Skip all tests during build
./mvnw clean install -DskipTests

# Alternative: Skip all tests using maven.test.skip
./mvnw clean install -Dmaven.test.skip=true
```

> Note: `-DskipTests` compiles tests but doesn't run them, while `-Dmaven.test.skip=true` skips both compilation and execution of tests.

### Skipping Integration Tests

#### Skip All Integration Tests for the Entire Project

```bash
# Skip all integration tests, run only unit tests
./mvnw clean install -P\!box-integration -P\!crud-integration

# Alternative: Run only the default unit test profile
./mvnw clean install -Punit
```

#### Skip Integration Tests for a Specific Module

```bash
# Skip integration tests for a specific module
./mvnw clean install -pl tarantool-client -P\!box-integration -P\!crud-integration

# Run only unit tests for a specific module
./mvnw clean install -pl tarantool-client -Punit
```

### Skipping Specific Tests

#### Skip an Exact Test Class or Method

```bash
# Skip a specific test class
./mvnw test -Dtest="!MyTestClass"

# Skip a specific test method within a class
./mvnw test -Dtest="MyTestClass#!myTestMethod"

# Run all tests except specific ones
./mvnw test -Dtest="!MyTestClass,!AnotherTestClass"
```

#### Using Test Categories

The project may use JUnit categories to group tests. You can include or exclude specific categories:

```bash
# Skip integration tests using Maven Surefire plugin configuration
./mvnw test -Dgroups="!integration"

# Run only specific test groups
./mvnw test -Dgroups="unit"
```

### Running Specific Tests

```bash
# Run a specific test class
./mvnw test -Dtest="MyTestClass"

# Run a specific test method
./mvnw test -Dtest="MyTestClass#myTestMethod"

# Run tests matching a pattern
./mvnw test -Dtest="*IntegrationTest"
```

### Test Profiles in the Project

The project defines several Maven profiles for different test scenarios:

- `unit` (default): Runs only unit tests
- `box-integration`: Runs box integration tests
- `crud-integration`: Runs CRUD integration tests

You can activate or deactivate profiles as needed for your testing requirements.

### Building Specific Modules

The project consists of multiple modules that can be built individually:

- `tarantool-core` - Core functionality
- `tarantool-client` - Main client library
- `tarantool-jackson-mapping` - Jackson-based serialization
- `tarantool-pooling` - Connection pooling
- `tarantool-balancer` - Load balancing
- `tarantool-schema` - Schema management
- `tarantool-spring-data` - Spring Data integration
- `testcontainers` - Test containers support

To build a specific module:
```bash
./mvnw clean install -pl tarantool-client -am -DskipTests
```

Where:
- `-pl` specifies the module to build
- `-am` builds also the required modules (aggregator modules)

## Verifying Installation

After installation, you can verify that the libraries are properly installed by checking:

1. Maven local repository:
   ```bash
   ls ~/.m2/repository/io/tarantool/
   ```

2. Using Maven dependency plugin:
   ```bash
   ./mvnw dependency:tree
   ```

## Troubleshooting

If you encounter issues during installation:

1. Ensure you have the required Java version (8 or higher)
2. Check that Maven is properly configured
3. Verify that you have sufficient disk space and permissions
4. Clean your local repository cache if needed:
   ```bash
   ./mvnw dependency:purge-local-repository
   ```

For snapshot versions, make sure your repository configuration allows snapshot updates.