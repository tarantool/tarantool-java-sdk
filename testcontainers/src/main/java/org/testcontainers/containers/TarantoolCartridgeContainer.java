/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
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
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.MountableFile;
import org.yaml.snakeyaml.Yaml;

public class TarantoolCartridgeContainer extends GenericContainer<TarantoolCartridgeContainer>
    implements ClusterContainer {

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

  private static final String TMP_DIR = "/tmp";

  /**
   * Runs a Lua command inside the container by connecting via net.box and returns YAML-encoded
   * output. This approach is used for Tarantool 2 / Cartridge clusters.
   */
  private static final String COMMAND_TEMPLATE =
      "echo \" "
          + "    print(require('yaml').encode( "
          + "        {require('net.box').connect( "
          + "            'localhost:%d',  "
          + "            { user = '%s', password = '%s' } "
          + "            ):eval('%s')}) "
          + "        ); "
          + "    os.exit(); "
          + "\" > /tmp/container-cmd.lua && tarantool /tmp/container-cmd.lua";

  private static final Yaml yaml = new Yaml();

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

  /**
   * Create a container with default image and specified instances file from the classpath
   * resources. Assumes that there is a file named Dockerfile in the project resources classpath.
   *
   * @param instancesFile path to instances.yml, relative to the classpath resources
   * @param topologyConfigurationFile path to a topology bootstrap script, relative to the classpath
   *     resources
   */
  public TarantoolCartridgeContainer(String instancesFile, String topologyConfigurationFile) {
    this(DOCKERFILE, instancesFile, topologyConfigurationFile);
  }

  /**
   * Create a container with default image and specified instances file from the classpath
   * resources. Assumes that there is a file named Dockerfile in the project resources classpath.
   *
   * @param instancesFile path to instances.yml, relative to the classpath resources
   * @param topologyConfigurationFile path to a topology bootstrap script, relative to the classpath
   *     resources
   * @param buildArgs a map of arguments that will be passed to docker ARG commands on image build.
   *     This values can be overridden by environment.
   */
  public TarantoolCartridgeContainer(
      String instancesFile, String topologyConfigurationFile, Map<String, String> buildArgs) {
    this(DOCKERFILE, "", instancesFile, topologyConfigurationFile, buildArgs);
  }

  /**
   * Create a container with default image and specified instances file from the classpath resources
   *
   * @param dockerFile path to a Dockerfile which configures Cartridge and other necessary services
   * @param instancesFile path to instances.yml, relative to the classpath resources
   * @param topologyConfigurationFile path to a topology bootstrap script, relative to the classpath
   *     resources
   */
  public TarantoolCartridgeContainer(
      String dockerFile, String instancesFile, String topologyConfigurationFile) {
    this(dockerFile, "", instancesFile, topologyConfigurationFile);
  }

  /**
   * Create a container with specified image and specified instances file from the classpath
   * resources. By providing the result Cartridge container image name, you can cache the image and
   * avoid rebuilding on each test run (the image is tagged with the provided name and not deleted
   * after tests finishing).
   *
   * @param dockerFile URL resource path to a Dockerfile which configures Cartridge and other
   *     necessary services
   * @param buildImageName Specify a stable image name for the test container to prevent rebuilds
   * @param instancesFile URL resource path to instances.yml relative in the classpath
   * @param topologyConfigurationFile URL resource path to a topology bootstrap script in the
   *     classpath
   */
  public TarantoolCartridgeContainer(
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

  /**
   * Create a container with specified image and specified instances file from the classpath
   * resources. By providing the result Cartridge container image name, you can cache the image and
   * avoid rebuilding on each test run (the image is tagged with the provided name and not deleted
   * after tests finishing).
   *
   * @param dockerFile URL resource path to a Dockerfile which configures Cartridge and other
   *     necessary services
   * @param buildImageName Specify a stable image name for the test container to prevent rebuilds
   * @param instancesFile URL resource path to instances.yml relative in the classpath
   * @param topologyConfigurationFile URL resource path to a topology bootstrap script in the
   *     classpath
   * @param buildArgs a map of arguments that will be passed to docker ARG commands on image build.
   *     This values can be overridden by environment.
   */
  public TarantoolCartridgeContainer(
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

  protected TarantoolCartridgeContainer(
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

  public TarantoolCartridgeContainer withFixedExposedPort(int hostPort, int containerPort) {
    super.addFixedExposedPort(hostPort, containerPort);
    return this;
  }

  public TarantoolCartridgeContainer withExposedPort(Integer port) {
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

  /**
   * Get the router host
   *
   * @return router hostname
   */
  public String getRouterHost() {
    return routerHost;
  }

  @Override
  public String getHost() {
    return super.getHost();
  }

  @Override
  public Integer getMappedPort(int originalPort) {
    return super.getMappedPort(originalPort);
  }

  /**
   * Get the router port
   *
   * @return router mapped port
   */
  @Override
  public int getPort() {
    if (useFixedPorts) {
      return routerPort;
    }
    return getMappedPort(routerPort);
  }

  public String getDirectoryBinding() {
    return directoryResourcePath;
  }

  /**
   * Specify the directory inside container that the resource directory will be mounted to. The
   * default value is "/app".
   *
   * @param instanceDir valid directory path
   * @return this container instance
   */
  public TarantoolCartridgeContainer withInstanceDir(String instanceDir) {
    checkNotRunning();
    this.instanceDir = instanceDir;
    return this;
  }

  public String getInstanceDir() {
    return instanceDir;
  }

  public int getInternalPort() {
    return routerPort;
  }

  /**
   * Get Cartridge router HTTP API hostname
   *
   * @return HTTP API hostname
   */
  public String getAPIHost() {
    return routerHost;
  }

  /** Checks if already running and if so raises an exception to prevent too-late setters. */
  protected void checkNotRunning() {
    if (isRunning()) {
      throw new IllegalStateException(
          "This option can be changed only before the container is running");
    }
  }

  /**
   * Specify the root directory of a Cartridge project relative to the resource classpath. The
   * default directory is the root resource directory.
   *
   * @param directoryResourcePath a valid directory path
   * @return this container instance
   */
  public TarantoolCartridgeContainer withDirectoryBinding(String directoryResourcePath) {
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

  /**
   * Get Cartridge router HTTP API port
   *
   * @return HTTP API port
   */
  public int getAPIPort() {
    if (useFixedPorts) {
      return apiPort;
    }
    return getMappedPort(apiPort);
  }

  /**
   * Use fixed ports binding. Defaults to false.
   *
   * @param useFixedPorts fixed ports for tarantool
   * @return HTTP API port
   */
  public TarantoolCartridgeContainer withUseFixedPorts(boolean useFixedPorts) {
    this.useFixedPorts = useFixedPorts;
    return this;
  }

  /**
   * Set Cartridge router hostname
   *
   * @param routerHost a hostname, default is "localhost"
   * @return this container instance
   */
  public TarantoolCartridgeContainer withRouterHost(String routerHost) {
    checkNotRunning();
    this.routerHost = routerHost;
    return this;
  }

  /**
   * Set Cartridge router binary port
   *
   * @param routerPort router Tarantool node port, usually 3301
   * @return this container instance
   */
  public TarantoolCartridgeContainer withRouterPort(int routerPort) {
    checkNotRunning();
    this.routerPort = routerPort;
    return this;
  }

  /**
   * Set Cartridge router HTTP API port
   *
   * @param apiPort HTTP API port, usually 8081
   * @return this container instance
   */
  public TarantoolCartridgeContainer withAPIPort(int apiPort) {
    checkNotRunning();
    this.apiPort = apiPort;
    return this;
  }

  /**
   * Set the username for accessing the router node
   *
   * @param routerUsername a user name, default is "admin"
   * @return this container instance
   */
  public TarantoolCartridgeContainer withRouterUsername(String routerUsername) {
    checkNotRunning();
    this.routerUsername = routerUsername;
    return this;
  }

  /**
   * Set the user password for accessing the router node
   *
   * @param routerPassword a user password, usually is a value of the "cluster_cookie" option in
   *     cartridge.cfg({...})
   * @return this container instance
   */
  public TarantoolCartridgeContainer withRouterPassword(String routerPassword) {
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
    // wait until Roles are configured
    waitUntilCartridgeIsHealthy(TIMEOUT_ROUTER_UP_CARTRIDGE_HEALTH_IN_SECONDS);
    bootstrapVshard();

    logger().info("Tarantool Cartridge cluster is started");
    logger().info("Tarantool Cartridge router is listening at {}:{}", getRouterHost(), getPort());
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
    try {
      ExecResult result = executeCommand(healthyCmd);
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
    try {
      ExecResult result = executeCommand(healthyCmd);
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

  public ExecResult executeScript(String scriptResourcePath) throws Exception {
    String scriptName = Paths.get(scriptResourcePath).getFileName().toString();
    String containerPath = normalizePath(Paths.get(TMP_DIR, scriptName));
    copyFileToContainer(MountableFile.forClasspathResource(scriptResourcePath), containerPath);
    return executeCommand(String.format("return dofile('%s')", containerPath));
  }

  public <T> T executeScriptDecoded(String scriptResourcePath) throws Exception {
    ExecResult result = executeScript(scriptResourcePath);

    if (result.getExitCode() != 0) {
      String message =
          String.format(
              "Executed script %s with exit code %d, stderr: \"%s\", stdout: \"%s\"",
              scriptResourcePath, result.getExitCode(), result.getStderr(), result.getStdout());

      if (result.getExitCode() == 3 || result.getExitCode() == 1) {
        throw new ExecutionException(message, new Throwable());
      }

      throw new IllegalStateException(message);
    }

    return yaml.load(result.getStdout());
  }

  @Override
  public ExecResult executeCommand(String command) {
    try {
      if (!isRunning()) {
        throw new IllegalStateException("Cannot execute commands in stopped container");
      }
      command = command.replace("\"", "\\\"").replace("\'", "\\\'");
      String bashCommand =
          String.format(COMMAND_TEMPLATE, routerPort, routerUsername, routerPassword, command);
      return execInContainer("sh", "-c", bashCommand);
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public <T> T executeCommandDecoded(String command) {
    ExecResult result = executeCommand(command);

    if (result.getExitCode() != 0) {
      throw new IllegalStateException(
          String.format(
              "Executed command \"%s\" with exit code %d, stderr: \"%s\", stdout: \"%s\"",
              command, result.getExitCode(), result.getStderr(), result.getStdout()));
    }

    return yaml.load(result.getStdout());
  }
}
