package scripts.githubcontextscripts;

import bio.terra.testrunner.runner.GitHubContextScript;
import bio.terra.testrunner.runner.GitHubContextScriptResult;
import java.util.Map;

public class GitHubRunId extends GitHubContextScript {

  private String runId;
  private String repoHtmlUrl;

  public static final String GITHUB_CONTEXT_RUN_ID = "GITHUB_CONTEXT_RUN_ID";
  public static final String GITHUB_CONTEXT_REPO_HTML_URL = "GITHUB_CONTEXT_REPO_HTML_URL";

  /**
   * Setter for any parameters required by the version script. These parameters will be set by the
   * Test Runner based on the current Test Configuration, and can be used by the version script
   * methods.
   *
   * @param parametersMap map of string key-value pairs supplied by the server.versionScripts
   *     configuration
   */
  @Override
  public void setParametersMap(Map<String, String> parametersMap) throws Exception {
    if (parametersMap == null || !parametersMap.containsKey(GITHUB_CONTEXT_RUN_ID)) {
      throw new IllegalArgumentException(
          "Must provide GITHUB_CONTEXT_RUN_ID env in the parameters list");
    }
    if (parametersMap == null || !parametersMap.containsKey(GITHUB_CONTEXT_REPO_HTML_URL)) {
      throw new IllegalArgumentException(
          "Must provide GITHUB_CONTEXT_REPO_HTML_URL env in the parameters list");
    }
    runId = parametersMap.get(GITHUB_CONTEXT_RUN_ID);
    repoHtmlUrl = parametersMap.get(GITHUB_CONTEXT_REPO_HTML_URL);
  }

  /** This method pulls data from the GitHub Workflow runtime context. */
  public GitHubContextScriptResult getGitHubWorkflowContext() throws Exception {
    return new GitHubContextScriptResult.Builder()
        .runId(System.getenv(runId))
        .repoHtmlUrl(System.getenv(repoHtmlUrl))
        .build();
  }
}
