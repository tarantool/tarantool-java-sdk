/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import static org.testcontainers.containers.utils.PathUtils.normalizePath;
import com.github.dockerjava.api.command.InspectContainerResponse;
import org.apache.commons.lang3.ArrayUtils;
import org.testcontainers.containers.utils.CartridgeConfigParser;
import org.testcontainers.containers.utils.SslContext;
import org.testcontainers.containers.utils.TarantoolContainerClientHelper;
import org.testcontainers.images.builder.ImageFromDockerfile;

public class CartridgeClusterContainer extends GenericContainer<CartridgeClusterContainer>
    implements ClusterContainer<CartridgeClusterContainer> {

  protected static final String ROUTER_HOST = "localhost";
  protected static final int ROUTER_PORT = 3301;
  protected static final String CARTRIDGE_DEFAULT_USERNAME = "admin";
  protected static final String CARTRIDGE_DEFAULT_PASSWORD = "secret-cluster-cookie";
  protected static final String DOCKERFILE = "Dockerfile";
  protected static final int API_PORT = 8081;
  protected static final String VSHARD_BOOTSTRAP_COMMAND =
      "return require('cartridge').admin_bootstrap_vshard()";
  protected static final String SCRIPT_RESOURCE_DIRECTORY = "";
  protected static final String INSTANCE_DIR = "/app";

  public static final String ENV_TARANTOOL_VERSION = "TARANTOOL_VERSION";
  public static final String ENV_TARANTOOL_SERVER_USER = "TARANTOOL_SERVER_USER";
  public static final String ENV_TARANTOOL_SERVER_UID = "TARANTOOL_SERVER_UID";
  public static final String ENV_TARANTOOL_SERVER_GROUP = "TARANTOOL_SERVER_GROUP";
  public static final String ENV_TARANTOOL_SERVER_GID = "TARANTOOL_SERVER_GID";
  public static final String ENV_TARANTOOL_WORKDIR = "TARANTOOL_WORKDIR";
  public static final String ENV_TARANTOOL_RUNDIR = "TARANTOOL_RUNDIR";
  public static final String ENV_TARANTOOL_LOGDIR = "TARANTOOL_LOGDIR";
  public static final String ENV_TARANTOOL_DATADIR = "TARANTOOL_DATADIR";
  public static final String ENV_TARANTOOL_INSTANCES_FILE = "TARANTOOL_INSTANCES_FILE";
  public static final String ENV_TARANTOOL_CLUSTER_COOKIE = "TARANTOOL_CLUSTER_COOKIE";
  protected static final String healthyCmd = "return require('cartridge').is_healthy()";
  protected static final int TIMEOUT_ROUTER_UP_CARTRIDGE_HEALTH_IN_SECONDS = 60;

  protected final CartridgeConfigParser instanceFileParser;
  protected final String TARANTOOL_RUN_DIR;

  protected boolean useFixedPorts = false;
  protected String routerHost = ROUTER_HOST;
  protected int routerPort = ROUTER_PORT;
  protected int apiPort = API_PORT;
  protected String routerUsername = CARTRIDGE_DEFAULT_USERNAME;
  protected String routerPassword = CARTRIDGE_DEFAULT_PASSWORD;
  protected String directoryResourcePath = SCRIPT_RESOURCE_DIRECTORY;
  protected String instanceDir = INSTANCE_DIR;
  protected String topologyConfigurationFile;
  protected String instancesFile;
  protected SslContext sslContext;

  public CartridgeClusterContainer(String instancesFile, String topologyConfigurationFile) {
    this(DOCKERFILE, instancesFile, topologyConfigurationFile);
  }

  public CartridgeClusterContainer(
      String instancesFile, String topologyConfigurationFile, Map<String, String> buildArgs) {
    this(DOCKERFILE, "", instancesFile, topologyConfigurationFile, buildArgs);
  }

  public CartridgeClusterContainer(
      String dockerFile, String instancesFile, String topologyConfigurationFile) {
    this(dockerFile, "", instancesFile, topologyConfigurationFile);
  }

  public CartridgeClusterContainer(
      String dockerFile,
      String buildImageName,
      String instancesFile,
      String topologyConfigurationFile) {
    this(
        dockerFile,
        buildImageName,
        instancesFile,
        topologyConfigurationFile,
        Collections.emptyMap());
  }

  public CartridgeClusterContainer(
      String dockerFile,
      String buildImageName,
      String instancesFile,
      String topologyConfigurationFile,
      final Map<String, String> buildArgs) {
    this(
        buildImage(dockerFile, buildImageName, buildArgs),
        instancesFile,
        topologyConfigurationFile,
        buildArgs);
  }

  protected CartridgeClusterContainer(
      ImageFromDockerfile image,
      String instancesFile,
      String topologyConfigurationFile,
      Map<String, String> buildArgs) {
    super(withBuildArgs(image, buildArgs));

    TARANTOOL_RUN_DIR =
        mergeBuildArguments(buildArgs).getOrDefault(ENV_TARANTOOL_RUNDIR, "/tmp/run");

    if (instancesFile == null || instancesFile.isEmpty()) {
      throw new IllegalArgumentException("Instance file name must not be null or empty");
    }
    if (topologyConfigurationFile == null || topologyConfigurationFile.isEmpty()) {
      throw new IllegalArgumentException("Topology configuration file must not be null or empty");
    }
    this.instancesFile = instancesFile;
    this.topologyConfigurationFile = topologyConfigurationFile;
    this.instanceFileParser = new CartridgeConfigParser(instancesFile);
  }

  protected static ImageFromDockerfile withBuildArgs(
      ImageFromDockerfile image, Map<String, String> buildArgs) {
    Map<String, String> args = mergeBuildArguments(buildArgs);

    if (!args.isEmpty()) {
      image.withBuildArgs(args);
    }

    return image;
  }

  public CartridgeClusterContainer withFixedExposedPort(int hostPort, int containerPort) {
    super.addFixedExposedPort(hostPort, containerPort);
    return this;
  }

  public CartridgeClusterContainer withExposedPort(Integer port) {
    super.addExposedPort(port);
    return this;
  }

  protected static Map<String, String> mergeBuildArguments(Map<String, String> buildArgs) {
    Map<String, String> args = new HashMap<>(buildArgs);

    for (String envVariable :
        Arrays.asList(
            ENV_TARANTOOL_VERSION,
            ENV_TARANTOOL_SERVER_USER,
            ENV_TARANTOOL_SERVER_UID,
            ENV_TARANTOOL_SERVER_GROUP,
            ENV_TARANTOOL_SERVER_GID,
            ENV_TARANTOOL_WORKDIR,
            ENV_TARANTOOL_RUNDIR,
            ENV_TARANTOOL_LOGDIR,
            ENV_TARANTOOL_DATADIR,
            ENV_TARANTOOL_INSTANCES_FILE,
            ENV_TARANTOOL_CLUSTER_COOKIE)) {
      String variableValue = System.getenv(envVariable);
      if (variableValue != null && !args.containsKey(envVariable)) {
        args.put(envVariable, variableValue);
      }
    }
    return args;
  }

  protected static ImageFromDockerfile buildImage(
      String dockerFile, String buildImageName, final Map<String, String> buildArgs) {
    ImageFromDockerfile image;
    if (buildImageName != null && !buildImageName.isEmpty()) {
      image = new ImageFromDockerfile(buildImageName, false);
    } else {
      image = new ImageFromDockerfile();
    }
    return image
        .withFileFromClasspath("Dockerfile", dockerFile)
        .withFileFromClasspath(
            "cartridge",
            buildArgs.get("CARTRIDGE_SRC_DIR") == null
                ? "cartridge"
                : buildArgs.get("CARTRIDGE_SRC_DIR"));
  }

  public String getRouterHost() {
    return routerHost;
  }

  public int getRouterPort() {
    if (useFixedPorts) {
      return routerPort;
    }
    return getMappedPort(routerPort);
  }

  public String getRouterUsername() {
    return routerUsername;
  }

  public String getRouterPassword() {
    return routerPassword;
  }

  @Override
  public String getHost() {
    return getRouterHost();
  }

  @Override
  public int getPort() {
    return getRouterPort();
  }

  @Override
  public String getUsername() {
    return getRouterUsername();
  }

  @Override
  public String getPassword() {
    return getRouterPassword();
  }

  @Override
  public String getDirectoryBinding() {
    return directoryResourcePath;
  }

  public CartridgeClusterContainer withInstanceDir(String instanceDir) {
    checkNotRunning();
    this.instanceDir = instanceDir;
    return this;
  }

  @Override
  public String getInstanceDir() {
    return instanceDir;
  }

  @Override
  public int getInternalPort() {
    return routerPort;
  }

  public String getAPIHost() {
    return routerHost;
  }

  protected void checkNotRunning() {
    if (isRunning()) {
      throw new IllegalStateException(
          "This option can be changed only before the container is running");
    }
  }

  public CartridgeClusterContainer withDirectoryBinding(String directoryResourcePath) {
    checkNotRunning();
    URL resource = getClass().getClassLoader().getResource(directoryResourcePath);
    if (resource == null) {
      throw new IllegalArgumentException(
          String.format(
              "No resource path found for the specified resource %s", directoryResourcePath));
    }
    this.directoryResourcePath = normalizePath(resource.getPath());
    return this;
  }

  public int getAPIPort() {
    if (useFixedPorts) {
      return apiPort;
    }
    return getMappedPort(apiPort);
  }

  public CartridgeClusterContainer withUseFixedPorts(boolean useFixedPorts) {
    this.useFixedPorts = useFixedPorts;
    return this;
  }

  public CartridgeClusterContainer withRouterHost(String routerHost) {
    checkNotRunning();
    this.routerHost = routerHost;
    return this;
  }

  public CartridgeClusterContainer withRouterPort(int routerPort) {
    checkNotRunning();
    this.routerPort = routerPort;
    return this;
  }

  public CartridgeClusterContainer withAPIPort(int apiPort) {
    checkNotRunning();
    this.apiPort = apiPort;
    return this;
  }

  public CartridgeClusterContainer withRouterUsername(String routerUsername) {
    checkNotRunning();
    this.routerUsername = routerUsername;
    return this;
  }

  public CartridgeClusterContainer withRouterPassword(String routerPassword) {
    checkNotRunning();
    this.routerPassword = routerPassword;
    return this;
  }

  @Override
  protected void configure() {
    if (!getDirectoryBinding().isEmpty()) {
      withFileSystemBind(getDirectoryBinding(), getInstanceDir(), BindMode.READ_WRITE);
    }
    if (useFixedPorts) {
      for (Integer port : instanceFileParser.getExposablePorts()) {
        addFixedExposedPort(port, port);
      }
    } else {
      addExposedPorts(ArrayUtils.toPrimitive(instanceFileParser.getExposablePorts()));
    }
  }

  @Override
  protected void containerIsStarting(InspectContainerResponse containerInfo) {
    logger().info("Tarantool Cartridge cluster is starting");
  }

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
                "cartridge",
                "replicasets",
                "--run-dir=" + TARANTOOL_RUN_DIR,
                "--file=" + replicasetsFileName,
                "--cfg=" + instancesFileName,
                "setup",
                "--bootstrap-vshard");
        if (result.getExitCode() != 0) {
          throw new RuntimeException(
              "Failed to change the app topology via cartridge CLI: " + result.getStdout());
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }

    } else {
      try {
        List<?> res = executeScriptDecoded(topologyConfigurationFile);
        if (res.size() >= 2 && res.get(1) != null && res.get(1) instanceof Map) {
          HashMap<?, ?> error = ((HashMap<?, ?>) res.get(1));
          return error.get("str").toString().contains("collision with another server");
        }
      } catch (Exception e) {
        if (e instanceof ExecutionException) {
          if (e.getCause() instanceof TimeoutException) {
            return true;
          }
        } else {
          throw new RuntimeException(e);
        }
      }
    }
    return true;
  }

  protected void retryingSetupTopology() {
    if (!setupTopology()) {
      try {
        logger().info("Retrying setup topology in 10 seconds");
        Thread.sleep(10_000);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      if (!setupTopology()) {
        throw new RuntimeException("Failed to change the app topology after retry");
      }
    }
  }

  protected void bootstrapVshard() {
    try {
      executeCommand(VSHARD_BOOTSTRAP_COMMAND);
    } catch (Exception e) {
      logger().error("Failed to bootstrap vshard cluster", e);
      throw new RuntimeException(e);
    }
  }

  @Override
  protected void containerIsStarted(InspectContainerResponse containerInfo, boolean reused) {
    super.containerIsStarted(containerInfo, reused);

    waitUntilRouterIsUp(TIMEOUT_ROUTER_UP_CARTRIDGE_HEALTH_IN_SECONDS);
    retryingSetupTopology();
    waitUntilCartridgeIsHealthy(TIMEOUT_ROUTER_UP_CARTRIDGE_HEALTH_IN_SECONDS);
    bootstrapVshard();

    logger().info("Tarantool Cartridge cluster is started");
    logger()
        .info("Tarantool Cartridge router is listening at {}:{}", getRouterHost(), getRouterPort());
    logger().info("Tarantool Cartridge HTTP API is available at {}:{}", getAPIHost(), getAPIPort());
  }

  protected void waitUntilRouterIsUp(int secondsToWait) {
    if (!waitUntilTrue(secondsToWait, this::routerIsUp)) {
      throw new RuntimeException(
          "Timeout exceeded during router starting stage." + " See the specific error in logs.");
    }
  }

  protected void waitUntilCartridgeIsHealthy(int secondsToWait) {
    if (!waitUntilTrue(secondsToWait, this::isCartridgeHealthy)) {
      throw new RuntimeException(
          "Timeout exceeded during cartridge topology applying stage."
              + " See the specific error in logs.");
    }
  }

  protected boolean waitUntilTrue(int secondsToWait, Supplier<Boolean> waitFunc) {
    int secondsPassed = 0;
    boolean result = waitFunc.get();
    while (!result && secondsPassed < secondsToWait) {
      result = waitFunc.get();
      try {
        Thread.sleep(1_000);
        secondsPassed++;
      } catch (InterruptedException e) {
        break;
      }
    }
    return result;
  }

  protected boolean routerIsUp() {
    ExecResult result;
    try {
      result = executeCommand(healthyCmd);
      if (result.getExitCode() != 0
          && result.getStderr().contains("Connection refused")
          && result.getStdout().isEmpty()) {
        return false;
      } else if (result.getExitCode() != 0) {
        logger()
            .error(
                "exit code: {}, stdout: {}, stderr: {}",
                result.getExitCode(),
                result.getStdout(),
                result.getStderr());
        return false;
      } else {
        return true;
      }
    } catch (Exception e) {
      logger().error(e.getMessage());
      return false;
    }
  }

  protected boolean isCartridgeHealthy() {
    ExecResult result;
    try {
      result = executeCommand(healthyCmd);
      if (result.getExitCode() != 0) {
        logger()
            .error(
                "exitCode: {}, stdout: {}, stderr: {}",
                result.getExitCode(),
                result.getStdout(),
                result.getStderr());
        return false;
      } else if (result.getStdout().startsWith("---\n- null\n")) {
        return false;
      } else if (result.getStdout().contains("true")) {
        return true;
      } else {
        logger()
            .warn(
                "exitCode: {}, stdout: {}, stderr: {}",
                result.getExitCode(),
                result.getStdout(),
                result.getStderr());
        return false;
      }
    } catch (Exception e) {
      logger().error("Error while waiting for cartridge healthy state: " + e.getMessage());
      return false;
    }
  }

  @Override
  public ExecResult executeScript(String scriptResourcePath) {
    try {
      return TarantoolContainerClientHelper.executeScript(
          this, scriptResourcePath, this.sslContext);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public <T> T executeScriptDecoded(String scriptResourcePath) {
    try {
      return TarantoolContainerClientHelper.executeScriptDecoded(
          this, scriptResourcePath, this.sslContext);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public ExecResult executeCommand(String command) {
    try {
      return TarantoolContainerClientHelper.executeCommand(this, command, this.sslContext);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public <T> T executeCommandDecoded(String command) {
    try {
      return TarantoolContainerClientHelper.executeCommandDecoded(this, command, this.sslContext);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
