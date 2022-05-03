package bio.terra.testrunner.runner;

public class TestRunnerEnvironmentScriptResult {
  public String githubRunId;
  public String githubRepository;
  public String githubServerUrl;

  public TestRunnerEnvironmentScriptResult(TestRunnerEnvironmentScriptResult.Builder builder) {
    this.githubRunId = builder.githubRunId;
    this.githubRepository = builder.githubRepository;
    this.githubServerUrl = builder.githubServerUrl;
  }

  public static class Builder {
    private String githubRunId;
    private String githubRepository;
    private String githubServerUrl;

    public TestRunnerEnvironmentScriptResult.Builder githubRunId(String githubRunId) {
      this.githubRunId = githubRunId;
      return this;
    }

    public TestRunnerEnvironmentScriptResult.Builder githubRepository(String githubRepository) {
      this.githubRepository = githubRepository;
      return this;
    }

    public TestRunnerEnvironmentScriptResult.Builder githubServerUrl(String githubServerUrl) {
      this.githubServerUrl = githubServerUrl;
      return this;
    }

    public TestRunnerEnvironmentScriptResult build() {
      return new TestRunnerEnvironmentScriptResult(this);
    }
  }
}
