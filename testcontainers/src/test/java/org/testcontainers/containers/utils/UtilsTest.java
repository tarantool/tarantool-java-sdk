/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package org.testcontainers.containers.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.apache.commons.io.file.PathUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class UtilsTest {

  @TempDir
  private static Path TEMP_DIR;

  @Test
  @EnabledIf(value = "isUnzipAvailable", disabledReason = "Утилита `unzip` не установлена на хосте")
  void testZipMethod() throws IOException {
    final Path zippingDir = TEMP_DIR.resolve("zip");

    final Path file1 = zippingDir.resolve("file1");
    final Path dir1 = zippingDir.resolve("dir1");
    final Path file2 = dir1.resolve("file2");
    final Path dir2 = dir1.resolve("dir2");
    final Path file3 = dir2.resolve("file3");
    final Path result = TEMP_DIR.resolve("result/result.zip");

    final Path unzippedDir = result.getParent().resolve("unzipped");
    final Path unzippedFile1 = unzippedDir.resolve("file1");
    final Path unzippedDir1 = unzippedDir.resolve("dir1");
    final Path unzippedFile2 = unzippedDir1.resolve("file2");
    final Path unzippedDir2 = unzippedDir1.resolve("dir2");
    final Path unzippedFile3 = unzippedDir2.resolve("file3");

    for (Path path : Arrays.asList(file1, file2, dir1, dir2, file3, result)) {
      PathUtils.createParentDirectories(path);
    }

    final List<Path> files = Arrays.asList(file1, file2, file3);
    for (int i = 0; i < files.size(); i++) {
      final Path file = files.get(i);
      Files.createFile(file);
      writeToFile(file, String.valueOf(i));
    }

    // check zip file is created
    Utils.zipDirectory(zippingDir, result);
    Assertions.assertFalse(PathUtils.isEmptyDirectory(result.getParent()));

    // unzipping via system unzip
    execUnzip(result, unzippedDir);

    final List<Path> unzippedFiles = Arrays.asList(unzippedFile1, unzippedFile2, unzippedFile3);
    for (int i = 0; i < unzippedFiles.size(); i++) {
      Assertions.assertTrue(PathUtils.directoryAndFileContentEquals(files.get(i), unzippedFiles.get(i)));
    }
  }

  private static void execUnzip(Path source, Path target) {
    try {
      final ProcessBuilder pb = new ProcessBuilder("unzip", source.toAbsolutePath().toString(), "-d",
          target.toAbsolutePath().toString());
      final Process process = pb.start();
      if (process.waitFor() != 0) {
        process.destroyForcibly();
        throw new RuntimeException("Process finished with exit code " + process.exitValue());
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static boolean isUnzipAvailable() {
    final Process process;
    try {
      process = new ProcessBuilder("unzip", "-v").start();
      return process.waitFor() == 0;
    } catch (IOException | InterruptedException e) {
      return false;
    }
  }

  private static void writeToFile(Path path, String content) throws IOException {
    Files.write(path, content.getBytes(StandardCharsets.UTF_8));
  }

  private static Stream<Arguments> positiveResolveContainerImageParams() {
    return Stream.of(
        // propertyName, defaultValue, expected
        Arguments.of("MY_IMAGE", "default", "my-registry/my-image:latest"),
        Arguments.of(null, "default", "default"),
        Arguments.of("", "default", "default"),
        Arguments.of("MY_IMAGE", "", "my-registry/my-image:latest"),
        Arguments.of(null, "default", "default"),
        Arguments.of("", "default", "default"),
        Arguments.of("EMPTY_VAR", "default", "default")
    );
  }

  @ParameterizedTest
  @MethodSource("positiveResolveContainerImageParams")
  void testResolveContainerImage_Positive(
      String propertyName,
      String defaultValue,
      String expected
  ) {
    // Arrange
    String propValue = null;
    if (propertyName != null && propertyName.equals("MY_IMAGE")) {
      propValue = "my-registry/my-image:latest";
      System.setProperty(propertyName, propValue);
    }

    // Act & Assert
    String result = Utils.resolveContainerImage(propertyName, defaultValue);
    assertEquals(expected, result);

    // Cleanup
    if (propertyName != null && !propertyName.isEmpty()) {
      System.clearProperty(propertyName);
    }
  }


  private static Stream<Arguments> negativeResolveContainerImageParams() {
    return Stream.of(
        // propertyName, defaultValue, expectedExceptionMessage
        Arguments.of(null, null),
        Arguments.of("", ""),
        Arguments.of("", null),
        Arguments.of(null, "")
    );
  }

  @ParameterizedTest
  @MethodSource("negativeResolveContainerImageParams")
  void testResolveContainerImage_Negative(
      String propertyName,
      String defaultValue
  ) {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> Utils.resolveContainerImage(propertyName, defaultValue));
    assertEquals("Both propertyName and defaultImageName are null or empty", ex.getMessage());
  }
}
