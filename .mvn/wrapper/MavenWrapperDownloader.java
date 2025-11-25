/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ThreadLocalRandom;

public final class MavenWrapperDownloader {
  private static final String WRAPPER_VERSION = "@@project.version@@";

  private static final boolean VERBOSE = Boolean.parseBoolean(System.getenv("MVNW_VERBOSE"));

  public static void main(String[] args) {
    log("Apache Maven Wrapper Downloader " + WRAPPER_VERSION);

    if (args.length != 2) {
      System.err.println(" - ERROR wrapperUrl or wrapperJarPath parameter missing");
      System.exit(1);
    }

    try {
      log(" - Downloader started");
      final URL wrapperUrl = URI.create(args[0]).toURL();
      final String jarPath = args[1].replace("..", ""); // Sanitize path
      final Path wrapperJarPath = Paths.get(jarPath).toAbsolutePath().normalize();
      downloadFileFromURL(wrapperUrl, wrapperJarPath);
      log("Done");
    } catch (IOException e) {
      System.err.println("- Error downloading: " + e.getMessage());
      if (VERBOSE) {
        e.printStackTrace();
      }
      System.exit(1);
    }
  }

  private static void downloadFileFromURL(URL wrapperUrl, Path wrapperJarPath) throws IOException {
    log(" - Downloading to: " + wrapperJarPath);
    if (System.getenv("MVNW_USERNAME") != null && System.getenv("MVNW_PASSWORD") != null) {
      final String username = System.getenv("MVNW_USERNAME");
      final char[] password = System.getenv("MVNW_PASSWORD").toCharArray();
      Authenticator.setDefault(
          new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
              return new PasswordAuthentication(username, password);
            }
          });
    }
    Path temp =
        wrapperJarPath
            .getParent()
            .resolve(
                wrapperJarPath.getFileName()
                    + "."
                    + Long.toUnsignedString(ThreadLocalRandom.current().nextLong())
                    + ".tmp");
    try (InputStream inStream = wrapperUrl.openStream()) {
      Files.copy(inStream, temp, StandardCopyOption.REPLACE_EXISTING);
      Files.move(temp, wrapperJarPath, StandardCopyOption.REPLACE_EXISTING);
    } finally {
      Files.deleteIfExists(temp);
    }
    log(" - Downloader complete");
  }

  private static void log(String msg) {
    if (VERBOSE) {
      System.out.println(msg);
    }
  }
}
