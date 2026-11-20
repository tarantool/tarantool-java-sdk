/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers.utils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class TarantoolContainerClientHelperTest {

  private static final String VSHARD_DOCKERFILE = "vshard_cluster/Dockerfile";

  @Test
  void testPrebuiltImageNameIsNullForNonRegistryNames() {
    assertNull(TarantoolContainerClientHelper.prebuiltImageName(null, VSHARD_DOCKERFILE, null));
    assertNull(TarantoolContainerClientHelper.prebuiltImageName("", VSHARD_DOCKERFILE, null));
    assertNull(
        TarantoolContainerClientHelper.prebuiltImageName(
            "vshard-cluster-java", VSHARD_DOCKERFILE, null));
    assertNull(
        TarantoolContainerClientHelper.prebuiltImageName(
            "registry.example.com/img", "Dockerfile", null));
  }

  @Test
  void testPrebuiltImageNameCarriesVersionAndContentHash() {
    String imageName =
        TarantoolContainerClientHelper.prebuiltImageName(
            "registry.example.com/img", VSHARD_DOCKERFILE, null);
    assertNotNull(imageName);
    assertTrue(
        imageName.matches("registry\\.example\\.com/img:.+-[0-9a-f]{12}"),
        "Unexpected image name: " + imageName);
  }

  @Test
  void testPrebuiltImageNameIsDeterministic() {
    assertEquals(
        TarantoolContainerClientHelper.prebuiltImageName(
            "registry.example.com/img", VSHARD_DOCKERFILE, null),
        TarantoolContainerClientHelper.prebuiltImageName(
            "registry.example.com/img", VSHARD_DOCKERFILE, null));
  }

  @Test
  void testPrebuiltImageNameDiffersBetweenResourceDirectories() {
    String vshardImageName =
        TarantoolContainerClientHelper.prebuiltImageName(
            "registry.example.com/img", VSHARD_DOCKERFILE, null);
    String cartridgeImageName =
        TarantoolContainerClientHelper.prebuiltImageName(
            "registry.example.com/img", "cartridge/Dockerfile", null);
    assertNotNull(vshardImageName);
    assertNotNull(cartridgeImageName);
    assertNotEquals(vshardImageName, cartridgeImageName);
  }

  @Test
  void testScriptOutsideResourceDirectoryForcesLocalBuild() {
    assertNull(
        TarantoolContainerClientHelper.prebuiltImageName(
            "registry.example.com/img", VSHARD_DOCKERFILE, List.of("custom/helpers.lua")));
    assertNull(
        TarantoolContainerClientHelper.prebuiltImageName(
            "registry.example.com/img", VSHARD_DOCKERFILE, List.of("")));
    assertNotNull(
        TarantoolContainerClientHelper.prebuiltImageName(
            "registry.example.com/img", VSHARD_DOCKERFILE, List.of("vshard_cluster/helpers.lua")));
  }

  /**
   * The Images workflow embeds a hash computed by a shell pipeline into the image tag; this test
   * runs the same pipeline over the same classpath directory and compares the result.
   */
  @Test
  void testContentHashMatchesCiShellPipeline() throws Exception {
    for (String directory : List.of("vshard_cluster", "cartridge")) {
      String imageName =
          TarantoolContainerClientHelper.prebuiltImageName(
              "registry.example.com/img", directory + "/Dockerfile", null);
      assertNotNull(
          imageName, "Resource directory " + directory + " is missing on the test classpath");
      String javaHash = imageName.substring(imageName.lastIndexOf('-') + 1);

      String directoryPath =
          new File(
                  TarantoolContainerClientHelperTest.class
                      .getClassLoader()
                      .getResource(directory)
                      .toURI())
              .getPath();
      Process process =
          new ProcessBuilder(
                  "sh",
                  "-c",
                  "find \"$1\" -type f -print0 | LC_ALL=C sort -z | xargs -0 cat | sha256sum",
                  "sh",
                  directoryPath)
              .start();
      String shellHash =
          new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8)
              .trim()
              .substring(0, 12);
      assertEquals(javaHash, shellHash, "Hash mismatch for directory " + directory);
    }
  }
}
