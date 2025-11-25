/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.ExecInContainerPattern;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.shaded.com.google.common.base.Strings;

public final class Utils {

  private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);

  Utils() {}

  public static Path createTempDirectory(String name) {
    try {
      if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
        final FileAttribute<?> attribute =
            PosixFilePermissions.asFileAttribute(EnumSet.allOf(PosixFilePermission.class));
        return Files.createTempDirectory(name, attribute).toRealPath();
      } else {
        return Files.createTempDirectory(name).toRealPath();
      }
    } catch (IOException e) {
      throw new IllegalStateException("Error creating temp config directory", e);
    }
  }

  public static void deleteDataDirectory(Path path) {
    if (path != null && Files.exists(path)) {
      try (Stream<Path> paths = Files.walk(path)) {
        paths
            .sorted(Comparator.reverseOrder())
            .forEach(
                p -> {
                  try {
                    LOGGER.debug("Deleting file {} ...", p);
                    Files.delete(p);
                    LOGGER.debug("File {} is deleted", p);
                  } catch (IOException e) {
                    LOGGER.debug("Error deleting file {}", p, e);
                  }
                });
      } catch (IOException e) {
        LOGGER.debug("Error deleting file {}", path, e);
      }
      return;
    }
    LOGGER.debug("Deleting file {} doesn't exists. Skipped", path);
  }

  public static String execExceptionally(
      Logger log, InspectContainerResponse info, String excMessage, String... command)
      throws IOException, InterruptedException {
    ExecResult execResult =
        ExecInContainerPattern.execInContainer(DockerClientFactory.lazyClient(), info, command);
    if (execResult.getExitCode() != 0) {
      throw new ContainerLaunchException(
          excMessage
              + ":\n\t\t[stderr]:\n"
              + execResult.getStderr()
              + "\n\t\t[stdout]:\n'"
              + execResult.getStdout()
              + "'");
    }
    log.info(
        "\n\t\t[stderr]:\n{}\n\t\t[stdout]:\n{}", execResult.getStderr(), execResult.getStdout());
    return execResult.getStdout();
  }

  public static boolean isNullOrBlank(String string) {
    return string == null || string.trim().isEmpty();
  }

  public static void zipDirectory(Path source, Path target) throws IOException {
    try (OutputStream fos = Files.newOutputStream(target);
        ZipOutputStream zos = new ZipOutputStream(fos)) {
      Files.walkFileTree(
          source,
          new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                throws IOException {
              if (!file.equals(target)) {
                Path targetFile = source.relativize(file);
                zos.putNextEntry(new ZipEntry(targetFile.toString().replace("\\", "/")));
                Files.copy(file, zos);
                zos.closeEntry();
              }
              return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                throws IOException {
              Path targetDir = source.relativize(dir);
              if (!targetDir.toString().isEmpty()) {
                zos.putNextEntry(new ZipEntry(targetDir.toString().replace("\\", "/") + "/"));
                zos.closeEntry();
              }
              return FileVisitResult.CONTINUE;
            }
          });
    }
  }

  /** Binds all exposed ports for passed container. */
  public static void bindExposedPorts(GenericContainer<?> container) {
    final ExposedPort[] exposedPorts = container.getContainerInfo().getConfig().getExposedPorts();
    if (exposedPorts == null || exposedPorts.length == 0) {
      return;
    }
    final List<String> binds =
        Arrays.stream(exposedPorts)
            .map(
                ex -> {
                  try {
                    return String.format(
                        "%d:%d", container.getMappedPort(ex.getPort()), ex.getPort());
                  } catch (IllegalArgumentException e) {
                    // Может быть такое, что в контейнеры есть порты, которые докер считает
                    // открытыми, а testcontainers о
                    // них не знает.
                    LOGGER.warn(
                        "Could not find mapped port for exposed docker port: {}. Skipping...",
                        ex.getPort());
                    return null;
                  }
                })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    container.setPortBindings(binds);
  }

  public static String resolveContainerImage(String propertyName, String defaultImageName) {
    String image = defaultImageName;

    if (!Strings.isNullOrEmpty(propertyName)) {
      String propertyValue = System.getProperty(propertyName);
      if (!Strings.isNullOrEmpty(propertyValue)) {
        image = propertyValue;
      }
    }
    if (!Strings.isNullOrEmpty(image)) {
      return image;
    }
    throw new IllegalArgumentException("Both propertyName and defaultImageName are null or empty");
  }
}
