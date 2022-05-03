package bio.terra.testrunner.runner;

import bio.terra.testrunner.runner.config.ServerSpecification;
import java.util.Map;

public class GitHubWorkflowScript {
  /** Public constructor so that this class can be instantiated via reflection. */
  public GitHubWorkflowScript() {}

  /**
   * Setter for any parameters required by the GitHub script. These parameters will be set by the
   * Test Runner based on the current Server Configuration, and can be used by the GitHub script
   * methods.
   *
   * @param parametersMap map of string key-value pairs supplied by the server.versionScripts
   *     configuration
   */
  public void setParametersMap(Map<String, String> parametersMap) throws Exception {}

  /**
   * The GitHub script getGitHubWorkflowContext method looks up the GitHub Action Workflow runtime
   * context.
   */
  public GitHubWorkflowScriptResult getGitHubWorkflowContext(ServerSpecification server)
      throws Exception {
    throw new UnsupportedOperationException(
        "getGitHubWorkflowContext must be overridden by sub-classes");
  }
}
