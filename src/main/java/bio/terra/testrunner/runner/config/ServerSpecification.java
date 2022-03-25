package bio.terra.testrunner.runner.config;

import bio.terra.testrunner.common.utils.FileUtils;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.List;
import org.apache.commons.lang.StringUtils;

/**
 * An instance of this class represents a single Terra environment or deployment. It contains all
 * the information a client would need to talk to the services. This includes the service URIs and
 * any additional information required to understand the connections between services (e.g.
 * credentials required to talk to a kernel-level service, resource id that represents the Data Repo
 * deployment in SAM).
 *
 * <p>Note: This class currently includes several properties specific to particular services (i.e.
 * everything under the "Terra services: ..." comment). I don't like this hard-coding because it
 * makes the TestRunner library "depend" on the services that it will be used to test. I think the
 * right way to do this is for all of these properties to be added to the Kubernetes config map and
 * removed from the ServerSpecification. We should only need the ClusterSpecification (i.e. the
 * pointer to the appropriate Kubernetes cluster) to get these values.
 */
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

  // External Credentials Manager
  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public String externalCredentialsManagerUri;

  // Drs Hub
  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public String drsHubUri;

  // Catalog Service
  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public String catalogUri;

  // =============================================
  // Cluster & deployment: information required to manipulate Kubernetes and deploy
  // Kubernetes cluster specification
  public ClusterSpecification cluster;

  // this service account has permissions to deploy, view Kubernetes, and query the metrics
  // and logs servers
  public String testRunnerServiceAccountFile;
  public ServiceAccountSpecification testRunnerServiceAccount;

  // This is an in-cluster service account with namespaced RBAC permissions to manipulate Kubernetes
  // With this, we no longer need Kubernetes admin role for the testRunnerServiceAccount.
  public String testRunnerK8SServiceAccountFile;
  public KubernetesServiceAccountSpecification testRunnerK8SServiceAccount;

  // how to (optionally) deploy before each test run
  public DeploymentScriptSpecification deploymentScript;

  // when set to true, all manipulations of Kubernetes are skipped
  public boolean skipKubernetes = false;

  // when set to true, there is no deploy before each test run
  public boolean skipDeployment = false;

  // =============================================
  // Version: information required to get the server version
  // how to (optionally) lookup the version before each test run
  public List<VersionScriptSpecification> versionScripts;

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

    // read in the test runner in-cluster service account file if configured
    if (StringUtils.isNotBlank(server.testRunnerK8SServiceAccountFile)) {
      server.testRunnerK8SServiceAccount =
          KubernetesServiceAccountSpecification.fromJSONFile(
              server.testRunnerK8SServiceAccountFile);
    }

    // read in the buffer service client service account file, if specified
    if (server.bufferClientServiceAccountFile != null
        && !server.bufferClientServiceAccountFile.isEmpty()) {
      server.bufferClientServiceAccount =
          ServiceAccountSpecification.fromJSONFile(server.bufferClientServiceAccountFile);
    }

    return server;
  }

  /** Validate the server specification read in from the JSON file. */
  public void validate() {
    if (!skipKubernetes) {
      if (cluster == null) {
        throw new IllegalArgumentException("Cluster Specification must be defined");
      }
      cluster.validate();
      if (testRunnerK8SServiceAccount == null) {
        throw new IllegalArgumentException(
            "Test Runner Kubernetes Service Account must be defined");
      }
      testRunnerK8SServiceAccount.validate();
    }
    if (!skipDeployment) {
      if (deploymentScript == null) {
        throw new IllegalArgumentException("Server deployment script must be defined");
      }
      deploymentScript.validate();
    }
    if (testRunnerServiceAccount == null) {
      throw new IllegalArgumentException("Test Runner Service Account must be defined");
    }
    testRunnerServiceAccount.validate();

    if (versionScripts != null && !versionScripts.isEmpty()) {
      versionScripts.forEach(versionScript -> versionScript.validate());
    }

    if (bufferClientServiceAccount != null) {
      bufferClientServiceAccount.validate();
    }
  }
}
