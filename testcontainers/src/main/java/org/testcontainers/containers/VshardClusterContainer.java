/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.testcontainers.containers.utils.PathUtils.normalizePath;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.HealthCheck;
import lombok.Getter;
import org.apache.commons.lang3.ArrayUtils;
import org.testcontainers.containers.utils.SslContext;
import org.testcontainers.containers.utils.TarantoolContainerClientHelper;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.yaml.snakeyaml.Yaml;

/**
 * @author Artyom Dubinin
 */
public class VshardClusterContainer extends GenericContainer<VshardClusterContainer>
    implements ClusterContainer<VshardClusterContainer> {

  protected static final String ROUTER_HOST = "localhost";
  protected static final int ROUTER_PORT = 3301;
  protected static final String VSHARD_CLUSTER_DEFAULT_USERNAME = "admin";
  protected static final String VSHARD_CLUSTER_DEFAULT_PASSWORD = "secret-cluster-cookie";
  protected static final String DOCKERFILE = "Dockerfile";
  protected static final String SCRIPT_RESOURCE_DIRECTORY = "";
  protected static final String INSTANCE_DIR = "/app";
  protected static final String APP_NAME = "vshard-cluster";
  protected static final String VSHARD_STARTUP_COMMAND_TEMPLATE =
      "cd %s && rm -f tt.yaml"
          + " && env -u TT_APP_NAME -u TT_INSTANCE_NAME tt init"
          + " && env -u TT_APP_NAME -u TT_INSTANCE_NAME tt build"
          + " && env -u TT_APP_NAME -u TT_INSTANCE_NAME tt start "
          + APP_NAME
          + " && sleep infinity";
  protected static final String VSHARD_BOOTSTRAP_COMMAND =
      "return require('vshard').router.bootstrap({if_not_bootstrapped = true})";
  protected static final String ROUTER_HEALTH_COMMAND = "return box.info.status";

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

  protected static final int TIMEOUT_CRUD_HEALTH_IN_SECONDS = 60;
  protected static final int TIMEOUT_ROUTER_HEALTH_IN_SECONDS = 90;
  protected static final int TIMEOUT_VSHARD_BOOTSTRAP_IN_SECONDS = 90;
  protected static final int TIMEOUT_CONTAINER_START_IN_SECONDS = 600;

  protected final String TARANTOOL_RUN_DIR;
  private final TarantoolConfigParser configParser;
  private final List<String> instanceNames;
  private final Lock lock;
  private boolean configured;

  protected boolean useFixedPorts = false;
  @Getter protected String routerHost = ROUTER_HOST;
  protected int routerPort = ROUTER_PORT;
  @Getter protected String routerUsername = VSHARD_CLUSTER_DEFAULT_USERNAME;
  @Getter protected String routerPassword = VSHARD_CLUSTER_DEFAULT_PASSWORD;
  protected String directoryResourcePath = SCRIPT_RESOURCE_DIRECTORY;
  protected String instanceDir = INSTANCE_DIR;
  protected String configFile;
  protected String instancesFile;
  protected SslContext sslContext;

  public VshardClusterContainer(String instancesFile, String configFile) {
    this(DOCKERFILE, instancesFile, configFile);
  }

  public VshardClusterContainer(
      String instancesFile, String configFile, Map<String, String> buildArgs) {
    this(DOCKERFILE, "", instancesFile, configFile, buildArgs);
  }

  public VshardClusterContainer(String dockerFile, String instancesFile, String configFile) {
    this(dockerFile, "", instancesFile, configFile);
  }

  public VshardClusterContainer(
      String dockerFile, String buildImageName, String instancesFile, String configFile) {
    this(dockerFile, buildImageName, instancesFile, configFile, Collections.emptyMap());
  }

  public VshardClusterContainer(
      String dockerFile,
      String buildImageName,
      String instancesFile,
      String configFile,
      final Map<String, String> buildArgs) {
    this(buildImage(dockerFile, buildImageName, buildArgs), instancesFile, configFile, buildArgs);
  }

  public VshardClusterContainer(
      final String dockerFile,
      final String buildImageName,
      final String instancesFile,
      final String configFile,
      final String baseImage) {
    this(
        buildImage(dockerFile, buildImageName, Arguments.get(baseImage, "enterprise")),
        instancesFile,
        configFile,
        Arguments.get(baseImage));
  }

  protected VshardClusterContainer(
      ImageFromDockerfile image,
      String instancesFile,
      String configFile,
      Map<String, String> buildArgs) {
    super(withBuildArgs(image, buildArgs));

    TARANTOOL_RUN_DIR =
        mergeBuildArguments(buildArgs).getOrDefault(ENV_TARANTOOL_RUNDIR, "/tmp/run");

    if (instancesFile == null || instancesFile.isEmpty()) {
      throw new IllegalArgumentException("Instance file name must not be null or empty");
    }
    if (configFile == null || configFile.isEmpty()) {
      throw new IllegalArgumentException("Configuration file must not be null or empty");
    }
    this.instancesFile = instancesFile;
    this.configFile = configFile;
    this.configParser = new TarantoolConfigParser(configFile);
    this.instanceNames = parseInstanceNames(instancesFile);
    this.lock = new ReentrantLock();
  }

  protected static ImageFromDockerfile withBuildArgs(
      ImageFromDockerfile image, Map<String, String> buildArgs) {
    Map<String, String> args = mergeBuildArguments(buildArgs);

    if (!args.isEmpty()) {
      image.withBuildArgs(args);
    }

    return image;
  }

  public VshardClusterContainer withFixedExposedPort(int hostPort, int containerPort) {
    super.addFixedExposedPort(hostPort, containerPort);
    return this;
  }

  public VshardClusterContainer withExposedPort(Integer port) {
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
            "cluster",
            buildArgs.get("CLUSTER_SRC_DIR") == null
                ? "cluster"
                : buildArgs.get("CLUSTER_SRC_DIR"));
  }

  public int getRouterPort() {
    if (useFixedPorts) {
      return routerPort;
    }
    return getMappedPort(routerPort);
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

  public VshardClusterContainer withInstanceDir(String instanceDir) {
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

  public VshardClusterContainer withDirectoryBinding(String directoryResourcePath) {
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

  public VshardClusterContainer withUseFixedPorts(boolean useFixedPorts) {
    this.useFixedPorts = useFixedPorts;
    return this;
  }

  public VshardClusterContainer withRouterHost(String routerHost) {
    checkNotRunning();
    this.routerHost = routerHost;
    return this;
  }

  public VshardClusterContainer withRouterPort(int routerPort) {
    checkNotRunning();
    this.routerPort = routerPort;
    return this;
  }

  public VshardClusterContainer withRouterUsername(String routerUsername) {
    checkNotRunning();
    this.routerUsername = routerUsername;
    return this;
  }

  public VshardClusterContainer withRouterPassword(String routerPassword) {
    checkNotRunning();
    this.routerPassword = routerPassword;
    return this;
  }

  @Override
  protected void configure() {
    try {
      this.lock.lock();

      if (this.configured) {
        return;
      }

      if (!getDirectoryBinding().isEmpty()) {
        withFileSystemBind(getDirectoryBinding(), getInstanceDir(), BindMode.READ_WRITE);
      }

      if (!instanceNames.isEmpty()) {
        withNetworkAliases(instanceNames.toArray(new String[0]));
      }

      if (useFixedPorts) {
        for (Integer port : configParser.getExposablePorts()) {
          addFixedExposedPort(port, port);
        }
      } else {
        addExposedPorts(ArrayUtils.toPrimitive(configParser.getExposablePorts()));
      }

      String configFileName = getFileName(configFile);
      withEnv("TT_CONFIG", instanceDir + "/vshard-cluster/" + configFileName);
      withStartupTimeout(Duration.ofSeconds(TIMEOUT_CONTAINER_START_IN_SECONDS));
      withPrivilegedMode(true);
      withCreateContainerCmdModifier(
          cmd -> {
            cmd.withUser("root")
                .withEntrypoint("sh")
                .withHealthcheck(new HealthCheck().withTest(List.of("NONE")));

            String[] env = cmd.getEnv();
            if (env != null && env.length > 0) {
              cmd.withEnv(
                  Arrays.stream(env)
                      .filter(e -> !e.startsWith("TT_APP_NAME="))
                      .filter(e -> !e.startsWith("TT_INSTANCE_NAME="))
                      .toArray(String[]::new));
            }
          });
      withCommand(
          "-ec", String.format(VSHARD_STARTUP_COMMAND_TEMPLATE, instanceDir + "/vshard-cluster"));

      this.configured = true;
    } catch (Exception e) {
      throw new ContainerLaunchException(e.getMessage(), e);
    } finally {
      this.lock.unlock();
    }
  }

  @Override
  protected void containerIsStarting(InspectContainerResponse containerInfo) {
    logger().info("Tarantool vshard cluster cluster is starting");
  }

  @Override
  protected void containerIsStarted(InspectContainerResponse containerInfo, boolean reused) {
    super.containerIsStarted(containerInfo, reused);

    waitUntilRouterIsUp(TIMEOUT_ROUTER_HEALTH_IN_SECONDS);
    waitUntilVshardIsBootstrapped(TIMEOUT_VSHARD_BOOTSTRAP_IN_SECONDS);
    waitUntilCrudIsUp(TIMEOUT_CRUD_HEALTH_IN_SECONDS);

    logger().info("Tarantool vshard cluster cluster is started");
    logger()
        .info(
            "Tarantool vshard cluster router is listening at {}:{}",
            getRouterHost(),
            getRouterPort());
  }

  protected void waitUntilCrudIsUp(int secondsToWait) {
    if (!waitUntilTrue(secondsToWait, this::crudIsUp)) {
      throw new RuntimeException(
          "Timeout exceeded during router starting stage." + " See the specific error in logs.");
    }
  }

  protected void waitUntilRouterIsUp(int secondsToWait) {
    if (!waitUntilTrue(secondsToWait, this::routerIsUp)) {
      throw new RuntimeException(
          "Timeout exceeded during router startup stage." + " See the specific error in logs.");
    }
  }

  protected void waitUntilVshardIsBootstrapped(int secondsToWait) {
    if (!waitUntilTrue(secondsToWait, this::vshardIsBootstrapped)) {
      throw new RuntimeException(
          "Timeout exceeded during vshard bootstrap stage." + " See the specific error in logs.");
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

  protected boolean crudIsUp() {
    ExecResult result;
    try {
      result = TarantoolContainerClientHelper.executeCommand(this, "return crud._VERSION", null);
      if (result.getExitCode() != 0) {
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

  protected boolean routerIsUp() {
    ExecResult result;
    try {
      result = TarantoolContainerClientHelper.executeCommand(this, ROUTER_HEALTH_COMMAND, null);
      if (result.getExitCode() != 0) {
        logger()
            .error(
                "exit code: {}, stdout: {}, stderr: {}",
                result.getExitCode(),
                result.getStdout(),
                result.getStderr());
        return false;
      }
      return result.getStdout().contains("running");
    } catch (Exception e) {
      logger().error(e.getMessage());
      return false;
    }
  }

  protected boolean vshardIsBootstrapped() {
    try {
      List<?> result =
          TarantoolContainerClientHelper.executeCommandDecoded(
              this, VSHARD_BOOTSTRAP_COMMAND, null);
      if (result.size() >= 2 && result.get(1) != null) {
        logger().warn("Vshard bootstrap is not ready yet: {}", result.get(1));
        return false;
      }
      return result.size() >= 1 && Boolean.TRUE.equals(result.get(0));
    } catch (Exception e) {
      logger().warn("Vshard bootstrap attempt failed: {}", e.getMessage());
      return false;
    }
  }

  protected String getFileName(String filePath) {
    if (filePath == null || filePath.isEmpty()) {
      throw new IllegalArgumentException("File path must not be null or empty");
    }
    int idx = filePath.lastIndexOf('/');
    return idx >= 0 ? filePath.substring(idx + 1) : filePath;
  }

  private List<String> parseInstanceNames(String instancesPath) {
    try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(instancesPath)) {
      if (inputStream == null) {
        throw new IllegalArgumentException(
            String.format("No resource path found for the specified resource %s", instancesPath));
      }
      Object parsed = new Yaml().load(inputStream);
      if (!(parsed instanceof Map<?, ?>)) {
        return Collections.emptyList();
      }
      return ((Map<?, ?>) parsed)
          .keySet().stream().map(String::valueOf).collect(Collectors.toList());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public ExecResult executeScript(String scriptResourcePath) {
    try {
      return TarantoolContainerClientHelper.executeScript(
          this, scriptResourcePath, this.sslContext);
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public <T> T executeScriptDecoded(String scriptResourcePath) {
    try {
      return TarantoolContainerClientHelper.executeScriptDecoded(
          this, scriptResourcePath, this.sslContext);
    } catch (IOException | InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public ExecResult executeCommand(String command) {
    try {
      return TarantoolContainerClientHelper.executeCommand(this, command, this.sslContext);
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public <T> T executeCommandDecoded(String command) {
    try {
      return TarantoolContainerClientHelper.executeCommandDecoded(this, command, this.sslContext);
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof VshardClusterContainer)) {
      return false;
    }

    VshardClusterContainer that = (VshardClusterContainer) o;
    return useFixedPorts == that.useFixedPorts
        && routerPort == that.routerPort
        && TARANTOOL_RUN_DIR.equals(that.TARANTOOL_RUN_DIR)
        && Objects.equals(configParser, that.configParser)
        && routerHost.equals(that.routerHost)
        && routerUsername.equals(that.routerUsername)
        && routerPassword.equals(that.routerPassword)
        && directoryResourcePath.equals(that.directoryResourcePath)
        && instanceDir.equals(that.instanceDir)
        && configFile.equals(that.configFile)
        && instancesFile.equals(that.instancesFile)
        && Objects.equals(sslContext, that.sslContext);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        super.hashCode(),
        TARANTOOL_RUN_DIR,
        configParser,
        useFixedPorts,
        routerHost,
        routerPort,
        routerUsername,
        routerPassword,
        directoryResourcePath,
        instanceDir,
        configFile,
        instancesFile,
        sslContext);
  }
}
