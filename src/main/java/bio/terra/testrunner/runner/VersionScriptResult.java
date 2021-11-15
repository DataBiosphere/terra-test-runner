package bio.terra.testrunner.runner;

import bio.terra.testrunner.runner.version.GitVersion;
import bio.terra.testrunner.runner.version.HelmVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;

@SuppressFBWarnings(
    value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD",
    justification = "This POJO class is used for easy serialization to JSON using Jackson.")
public class VersionScriptResult {
  public List<HelmVersion> helmVersions;
  public List<GitVersion> gitVersions;
  // TODO: add more fields here for other version information we may care about

  // public VersionScriptResult() {}

  public VersionScriptResult(Builder builder) {
    this.helmVersions = builder.helmVersions;
    this.gitVersions = builder.gitVersions;
  }

  public static class Builder {
    private List<HelmVersion> helmVersions;
    private List<GitVersion> gitVersions;

    public VersionScriptResult.Builder helmVersions(List<HelmVersion> helmVersions) {
      this.helmVersions = helmVersions;
      return this;
    }

    public VersionScriptResult.Builder gitVersions(List<GitVersion> gitVersions) {
      this.gitVersions = gitVersions;
      return this;
    }

    public VersionScriptResult build() {
      return new VersionScriptResult(this);
    }
  }

  // TODO: add more overloaded add methods for other version sources

  // TODO: QA-1643 Re-enable or add builder for importComponentVersions API route pending DevOps
  // readiness
  // public Map<String, Map<String, String>> kubernetesComponentVersions;
}
