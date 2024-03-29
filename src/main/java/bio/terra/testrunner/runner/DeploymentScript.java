package bio.terra.testrunner.runner;

import bio.terra.testrunner.runner.config.ApplicationSpecification;
import bio.terra.testrunner.runner.config.ServerSpecification;
import java.util.Map;

public abstract class DeploymentScript {
  /** Public constructor so that this class can be instantiated via reflection. */
  public DeploymentScript() {}

  /**
   * Setter for any parameters required by the deployment script. These parameters will be set by
   * the Test Runner based on the current Test Configuration, and can be used by the Deployment
   * script methods.
   *
   * @param parametersMap map of string key-value pairs supplied by the server.deploymentScript
   *     configuration
   */
  public void setParametersMap(Map<String, String> parametersMap) throws Exception {}

  /** The deployment script deploy method kicks off the deployment. */
  public void deploy(ServerSpecification server, ApplicationSpecification app) throws Exception {
    throw new UnsupportedOperationException("deploy must be overridden by sub-classes");
  }

  /**
   * The deployment script waitForDeployToFinish method polls until the new deployment is running
   * successfully. This means that it is returning success to status checks and that the deployment
   * has switched over to the new image/application properties/etc. (if applicable).
   */
  public void waitForDeployToFinish() throws Exception {
    throw new UnsupportedOperationException(
        "waitForDeployToFinish must be overridden by sub-classes");
  }

  /**
   * The deployment script teardown method deletes or resets the deployment. Sub-classes may leave
   * this blank to leave the deployment as is at the end of a test run. This may be helpful for
   * debugging. From a reproducibility standpoint, re-deploying at the beginning of a new test run
   * is more important.
   */
  public void teardown() throws Exception {}
}
