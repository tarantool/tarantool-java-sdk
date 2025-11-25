# How to Contribute

## License

By contributing, you agree that your contributions will be licensed under the project's license.
Please read the [LICENSE](LICENSE.md) file before submitting any contributions.

## Getting Started

Thank you for your interest in contributing to the Tarantool Java SDK! This guide details how to contribute
to this library in a way that is easy for everyone. These are mostly guidelines, not rules.
Use your best judgement and feel free to propose changes to this document in a merge request.

### Reporting Issues

Please do:
* Check existing issues to verify that the bug or feature request has not already been submitted.
* Open an issue if things aren't working as expected.
* Open an issue to propose a significant change.
* Open an issue to propose a feature.

Use the provided **GitHub issue templates** when reporting bugs or requesting features.

### Setting Up the Development Environment

- **Java 17**
- **Maven 3.9.11+**
- **lefthook** to automatically format code using Google Java Style (Spotless plugin).

### Making Changes

We use the [fork and pull](https://docs.github.com/en/pull-requests/collaborating-with-pull-requests/getting-started/about-collaborative-development-models#fork-and-pull-model)
model for contributions.

- Fork the repository under your own GitHub account.
- Clone your personal fork to your local machine using the `git clone` command.
- Make changes, add tests, and ensure all tests pass.
- Add a [changelog](CHANGELOG.md) entry.
- Add or update Javadoc for all new classes and methods.
- Add or update documentation if needed.

### Submitting Changes

#### Pull Request Guidelines

- Clean up your branch before pushing to keep only appropriate commits, preferably just one.
- Make sure your PR is up to date with the `master` branch.
- Push changes and open **Pull Request** for maintainer's review with following merge into the main project.
- Wait for CI and fix all problems.

#### Commit Message Guidelines

- Use the [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/) style for all commit messages.
- Clearly describe the reason for the change and why it is necessary.

### Code Review Guidelines

Once you receive a review, follow these suggestions to make the process comfortable for everyone:

- Don't add fixup commits on top of the initial patchset.
  Squash fixups into appropriate commits and force-push your branch.
  Your patchset will be merged into `master` as is, without any squashing or reformatting.
  Keep it in good shape.
- Take into account comments and fix them. If you disagree with a comments describe your arguments or doubts.
- Work in iterations. Either process all comments at once or mark the pull request as a draft and return it back
  when all comments have been addressed.
  A reviewer always wants to just look and say "everything is nice" rather than request changes
  and remind about forgotten things.
- If you run out of spare time, mark the pull request as draft or close it.

## Project Structure

```
tarantool-java-sdk (parent POM)
├── tarantool-core
├── tarantool-jackson-mapping
├── tarantool-client
├── tarantool-pooling
├── tarantool-balancer
├── tarantool-schema
├── tarantool-spring-data
    ├── tarantool-spring-data-core    
    ├── tarantool-spring-data-27    
    ├── ...
    ├── tarantool-spring-data-34
├── testcontainers
└── jacoco-coverage-aggregate-report
```

1. tarantool-core - Core protocol implementation and basic data structures for communicating with Tarantool database
2. tarantool-jackson-mapping - JSON serialization/deserialization using Jackson for Tarantool data mapping
3. tarantool-client - Main client implementation with connection handling and basic operations
4. tarantool-pooling - Connection pooling functionality for managing multiple Tarantool connections efficiently
5. tarantool-balancer - Load balancing capabilities for distributed Tarantool instances
6. tarantool-schema - Schema management and validation tools for Tarantool spaces and indexes
7. tarantool-spring-data - Spring Data integration for Tarantool, providing repository abstractions
8. testcontainers - Testing utilities using Testcontainers for integration testing with Tarantool
9. jacoco-coverage-aggregate-report - Code coverage aggregation for all modules

## Asking Questions

- Specify preferred channels for asking questions (e.g., GitHub Discussions, chat platforms).

## Maintainer Information

See [MAINTAINERS.md](MAINTAINERS.md)