package bio.terra.testrunner.runner;

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

  // Builder for helm versions
  public static class HelmVersion {
    public String appName;
    public String helmAppVersion;
    public String helmChartVersion;

    public HelmVersion(Builder builder) {
      this.appName = builder.appName;
      this.helmAppVersion = builder.helmAppVersion;
      this.helmChartVersion = builder.helmChartVersion;
    }

    public static class Builder {
      private String appName;
      private String helmAppVersion;
      private String helmChartVersion;

      public Builder appName(String appName) {
        this.appName = appName;
        return this;
      }

      public Builder helmAppVersion(String helmAppVersion) {
        this.helmAppVersion = helmAppVersion;
        return this;
      }

      public Builder helmChartVersion(String helmChartVersion) {
        this.helmChartVersion = helmChartVersion;
        return this;
      }

      public HelmVersion build() {
        return new HelmVersion(this);
      }
    }
  }

  // Builder for git versions
  public static class GitVersion {
    public String remoteOriginUrl;
    public String branch;
    public String refHeadCommit;
    public String shortRefHeadCommit;

    public GitVersion(Builder builder) {
      this.remoteOriginUrl = builder.remoteOriginUrl;
      this.branch = builder.branch;
      this.refHeadCommit = builder.refHeadCommit;
      this.shortRefHeadCommit = builder.shortRefHeadCommit;
    }

    public static class Builder {
      private String remoteOriginUrl;
      private String branch;
      private String refHeadCommit;
      private String shortRefHeadCommit;

      public Builder remoteOriginUrl(String remoteOriginUrl) {
        this.remoteOriginUrl = remoteOriginUrl;
        return this;
      }

      public Builder branch(String branch) {
        this.branch = branch;
        return this;
      }

      public Builder refHeadCommit(String refHeadCommit) {
        this.refHeadCommit = refHeadCommit;
        return this;
      }

      public Builder shortRefHeadCommit(String shortRefHeadCommit) {
        this.shortRefHeadCommit = shortRefHeadCommit;
        return this;
      }

      public GitVersion build() {
        return new GitVersion(this);
      }
    }
  }

  // TODO: QA-1643 Re-enable or add builder for importComponentVersions API route pending DevOps
  // readiness
  // public Map<String, Map<String, String>> kubernetesComponentVersions;
}
