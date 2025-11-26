# How to Contribute

## License

By contributing, you agree that your contributions will be licensed under the project's license.  
Please read the [LICENSE](LICENSE) file before submitting any contributions.

## Getting Started

Thank you for your interest in contributing to the Tarantool Java SDK! This guide details how to contribute
to this extension in a way that is easy for everyone. These are mostly guidelines, not rules.
Use your best judgement, and feel free to propose changes to this document in a merge request.

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
- Push changes to your GitHub fork repository and open a **Pull Request** to propose
  that the original repository's maintainers merge your changes into the main project.
- Wait for CI and fix all problems.

#### Commit Message Guidelines

- Use the [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/) style for all commit messages.
- Clearly describe the reason for the change and why it is necessary.

### Code Review Guidelines

Once you receive a review, follow these suggestions to make the process comfortable for everyone:

- Don't add fixup commits on top of the initial patchset.
  Squash fixups into appropriate commits and force-push your branch.
  Your patchset will land in `master` as is, without any squashing or reformatting.
  Keep it in good shape.
- React to comments and respond with a summary of changes.
  If you disagree with a comment, describe your arguments or doubts.
- Work in iterations. Either process all comments at once or mark the pull request as a draft and return it back
  when all comments have been addressed.
  A reviewer always wants to just look and say "everything is nice" rather than request changes
  and remind about forgotten things.
- If you run out of spare time, mark the pull request as draft or close it.

## Project Structure

- Briefly explain the purpose of each module in the project. This helps contributors understand the architecture and where to make changes.

## Asking Questions

- Specify preferred channels for asking questions (e.g., GitHub Discussions, chat platforms).

## Maintainer Information

See [MAINTAINERS.md](MAINTAINERS.md)