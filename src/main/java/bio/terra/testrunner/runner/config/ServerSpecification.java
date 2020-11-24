package bio.terra.testrunner.runner.config;

import bio.terra.testrunner.common.utils.FileUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;

public class ServerSpecification implements SpecificationInterface {
  public String name;
  public String description = "";

  // =============================================
  // Terra services: information required to hit service endpoints
  // SAM-related fields
  public String samUri;

  // Data Repo-related fields
  public String datarepoUri;
  public String samResourceIdForDatarepo;

  // Buffer Service-related fields
  public String bufferUri;
  public String bufferClientServiceAccountFile;
  public ServiceAccountSpecification bufferClientServiceAccount;

  // Workspace Manager-related fields
  public String workspaceManagerUri;

  // =============================================
  // Cluster & deployment: information required to manipulate Kubernetes and deploy
  // Kubernetes cluster specification
  public ClusterSpecification cluster;

  // this service account has permissions to deploy, manipulate Kubernetes, and query the metrics
  // and logs servers
  public String testRunnerServiceAccountFile;
  public ServiceAccountSpecification testRunnerServiceAccount;

  // how to (optionally) deploy before each test run
  public DeploymentScriptSpecification deploymentScript;

  // when set to true, all manipulations of Kubernetes are skipped
  public boolean skipKubernetes = false;

  // when set to true, there is no deploy before each test run
  public boolean skipDeployment = false;

  public static final String resourceDirectory = "servers";

  ServerSpecification() {}

  /**
   * Read an instance of this class in from a JSON-formatted file. This method expects that the file
   * name exists in the directory specified by {@link #resourceDirectory}
   *
   * @param resourceFileName file name
   * @return an instance of this class
   */
  public static ServerSpecification fromJSONFile(String resourceFileName) throws Exception {
    // use Jackson to map the stream contents to a TestConfiguration object
    ObjectMapper objectMapper = new ObjectMapper();

    // read in the server file
    InputStream inputStream =
        FileUtils.getResourceFileHandle(resourceDirectory + "/" + resourceFileName);
    ServerSpecification server = objectMapper.readValue(inputStream, ServerSpecification.class);

    // read in the test runner service account file
    server.testRunnerServiceAccount =
        ServiceAccountSpecification.fromJSONFile(server.testRunnerServiceAccountFile);

    // read in the buffer service client service account file, if specified
    if (server.bufferClientServiceAccountFile != null
        && !server.bufferClientServiceAccountFile.isEmpty()) {
      server.bufferClientServiceAccount =
          ServiceAccountSpecification.fromJSONFile(server.bufferClientServiceAccountFile);
    }

    return server;
  }

  /**
   * Validate the server specification read in from the JSON file. None of the properties should be
   * null.
   */
  public void validate() {
    if (!skipKubernetes) {
      cluster.validate();
    }
    if (!skipDeployment) {
      if (deploymentScript == null) {
        throw new IllegalArgumentException("Server deployment script must be defined");
      }
      deploymentScript.validate();
    }

    testRunnerServiceAccount.validate();

    if (bufferClientServiceAccount != null) {
      bufferClientServiceAccount.validate();
    }
  }
}
