/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.client.integration;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.testcontainers.containers.Arguments;
import org.testcontainers.containers.TarantoolCartridgeContainer;
import org.testcontainers.containers.exceptions.CartridgeTopologyException;

public class TarantoolDBContainer extends TarantoolCartridgeContainer {

  public TarantoolDBContainer(String instancesFile, String topologyConfigurationFile) {
    super(instancesFile, topologyConfigurationFile);
  }

  public TarantoolDBContainer(
      String instancesFile, String topologyConfigurationFile, Map<String, String> buildArgs) {
    super(instancesFile, topologyConfigurationFile, buildArgs);
  }

  public TarantoolDBContainer(
      String dockerFile, String instancesFile, String topologyConfigurationFile) {
    super(dockerFile, instancesFile, topologyConfigurationFile);
  }

  public TarantoolDBContainer(
      String dockerFile,
      String buildImageName,
      String instancesFile,
      String topologyConfigurationFile) {
    super(dockerFile, buildImageName, instancesFile, topologyConfigurationFile);
  }

  public TarantoolDBContainer(
      String dockerFile,
      String buildImageName,
      String instancesFile,
      String topologyConfigurationFile,
      String baseImage) {
    super(
        dockerFile,
        buildImageName,
        instancesFile,
        topologyConfigurationFile,
        Arguments.get(baseImage, "enterprise"));
  }

  @Override
  protected void containerIsStarted(InspectContainerResponse containerInfo, boolean reused) {
    super.containerIsStarted(containerInfo, reused);
    try {
      execInContainer("bash", "upload_migrations.sh");
      executeCommand("return require('migrator').up()");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void waitUntilInstancesAreHealthy() {
    waitUntilCartridgeIsHealthy(TIMEOUT_ROUTER_UP_CARTRIDGE_HEALTH_IN_SECONDS);
  }

  public boolean areInstancesHealthy() {
    return isCartridgeHealthy();
  }

  public void startInstances() {
    try {
      execInContainer("tt", "start", "tarantooldb");
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public void stopInstance(String instanceName) {
    try {
      execInContainer("tt", "stop", "tarantooldb:" + instanceName);
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected boolean setupTopology() {
    String fileType =
        topologyConfigurationFile.substring(topologyConfigurationFile.lastIndexOf('.') + 1);
    if (fileType.equals("yml")) {
      String replicasetsFileName =
          topologyConfigurationFile.substring(topologyConfigurationFile.lastIndexOf('/') + 1);
      String instancesFileName = instancesFile.substring(instancesFile.lastIndexOf('/') + 1);
      try {
        ExecResult result =
            execInContainer(
                "tt",
                "cartridge",
                "replicasets",
                "setup",
                "--run-dir=" + TARANTOOL_RUN_DIR,
                "--file=" + replicasetsFileName,
                "--cfg=" + instancesFileName,
                "--bootstrap-vshard");
        if (result.getExitCode() != 0) {
          throw new CartridgeTopologyException(
              "Failed to change the app topology via tt CLI: "
                  + result.getStdout()
                  + " "
                  + result.getStderr());
        }
      } catch (Exception e) {
        throw new CartridgeTopologyException(e);
      }

    } else {
      try {
        List<?> res = executeScriptDecoded(topologyConfigurationFile);
        if (res.size() >= 2 && res.get(1) != null && res.get(1) instanceof Map) {
          HashMap<?, ?> error = ((HashMap<?, ?>) res.get(1));
          // that means topology already exists
          return error.get("str").toString().contains("collision with another server");
        }
        // The client connection will be closed after that command
      } catch (Exception e) {
        if (e instanceof ExecutionException) {
          if (e.getCause() instanceof TimeoutException) {
            return true;
            // Do nothing, the cluster is reloading
          }
        } else {
          throw new CartridgeTopologyException(e);
        }
      }
    }
    return true;
  }
}
