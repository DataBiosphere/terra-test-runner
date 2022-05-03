package bio.terra.testrunner.runner;

public class GitHubContextScriptResult {
  public String runId;
  public String repoHtmlUrl;

  public GitHubContextScriptResult(GitHubContextScriptResult.Builder builder) {
    this.runId = builder.runId;
    this.repoHtmlUrl = builder.repoHtmlUrl;
  }

  public static class Builder {
    private String runId;
    private String repoHtmlUrl;

    public GitHubContextScriptResult.Builder runId(String runId) {
      this.runId = runId;
      return this;
    }

    public GitHubContextScriptResult.Builder repoHtmlUrl(String repoHtmlUrl) {
      this.repoHtmlUrl = repoHtmlUrl;
      return this;
    }

    public GitHubContextScriptResult build() {
      return new GitHubContextScriptResult(this);
    }
  }
}
