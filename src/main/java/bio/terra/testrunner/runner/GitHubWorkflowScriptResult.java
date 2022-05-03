package bio.terra.testrunner.runner;

public class GitHubWorkflowScriptResult {
  public String runId;
  public String repoHtmlUrl;

  public GitHubWorkflowScriptResult(GitHubWorkflowScriptResult.Builder builder) {
    this.runId = builder.runId;
    this.repoHtmlUrl = builder.repoHtmlUrl;
  }

  public static class Builder {
    private String runId;
    private String repoHtmlUrl;

    public GitHubWorkflowScriptResult.Builder runId(String runId) {
      this.runId = runId;
      return this;
    }

    public GitHubWorkflowScriptResult.Builder repoHtmlUrl(String repoHtmlUrl) {
      this.repoHtmlUrl = repoHtmlUrl;
      return this;
    }

    public GitHubWorkflowScriptResult build() {
      return new GitHubWorkflowScriptResult(this);
    }
  }
}
