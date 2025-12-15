# Release Process for Tarantool Java SDK

Follow these steps to prepare and publish a new release:

1. **Update JavaDoc and Project Documentation**
    - Review and update all JavaDoc comments.
    - Ensure that the generated Javadoc (`mvn javadoc:javadoc`) builds without errors.
    - Update all relevant project documentation files (e.g., `README.md`, module docs).

2. **Create a Release Preparation Branch**
    - Create a new branch for the release preparation.
    - In this branch, make the following changes:
        - Update `CHANGELOG.md`: set the release version in format `X.Y.Z` and release date and ensure all significant changes are clearly described.
        - Update `README.md` and any other files where the library version is mentioned.
    - Commit your changes.
    - Open a Pull Request (PR) to `master` with your updates.

3. **Verify CI/CD Status**
    - Ensure that all CI/CD checks (tests, Javadoc generation, code style, etc.) pass successfully for your PR.

4. **Run the "Prepare Release" Workflow**
    - After the PR is merged, trigger the "Prepare Release" workflow (using Maven Release Plugin) to prepare the release.
    - This will update versions, tag the release, and push changes to the repository.

5. **Publish Artifacts to Maven Central**
    - Once the workflow completes successfully, go to Maven Central and publish the release from the service account.

6. **Create a GitHub Release**
    - Manually create a new release on GitHub.
    - Use the information from `CHANGELOG.md` for the release notes.

7. **Announce the Release**
    - Publish release information in all related communication channels (e.g., mailing lists, chat platforms, project website, social media, etc.) so that users and contributors are informed about the new version.

---

**Notes:**
- Never skip updating JavaDoc, documentation, or the changelog.
- If any step fails, address the issue and repeat the process as needed.
- For more details on the Maven Release Plugin, see the [official documentation](https://maven.apache.org/maven-release/maven-release-plugin/).