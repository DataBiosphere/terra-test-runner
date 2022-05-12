package scripts.runtimeenvscripts;

import bio.terra.testrunner.runner.TestRunnerEnvironmentScript;
import bio.terra.testrunner.runner.TestRunnerEnvironmentScriptResult;

public class GitHubActionsWorkflowRunContext extends TestRunnerEnvironmentScript {

  public static final String GITHUB_RUN_ID = "GITHUB_RUN_ID";
  public static final String GITHUB_REPOSITORY = "GITHUB_REPOSITORY";
  public static final String GITHUB_SERVER_URL = "GITHUB_SERVER_URL";

  /** This method pulls data from the GitHub Workflow runtime context. */
  public TestRunnerEnvironmentScriptResult getTestRunnerEnvironmentContext() throws Exception {
    return new TestRunnerEnvironmentScriptResult.Builder()
        .githubRunId(System.getenv(GITHUB_RUN_ID))
        .githubRepository(System.getenv(GITHUB_REPOSITORY))
        .githubServerUrl(System.getenv(GITHUB_SERVER_URL))
        .build();
  }
}
