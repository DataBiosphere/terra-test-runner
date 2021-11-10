package bio.terra.testrunner.runner;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings(
    value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD",
    justification = "This POJO class is used for easy serialization to JSON using Jackson.")
public class VersionScriptResult {
  // TODO: add more fields here for other version information we may care about
  //  (e.g. git hashes, helm versions for other services)
  public String appName;
  public String helmAppVersion;
  public String helmChartVersion;

  // TODO: QA-1643 Re-enable importComponentVersions API route pending DevOps readiness
  // public Map<String, Map<String, String>> kubernetesComponentVersions;

  public VersionScriptResult(Builder builder) {
    this.appName = builder.appName;
    this.helmAppVersion = builder.helmAppVersion;
    this.helmChartVersion = builder.helmChartVersion;
    // this.kubernetesComponentVersions = builder.kubernetesComponentVersions;
  }

  public static class Builder {
    private String appName;
    private String helmAppVersion;
    private String helmChartVersion;
    // private Map<String, Map<String, String>> kubernetesComponentVersions;

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

    // TODO: QA-1643 Re-enable importComponentVersions API route pending DevOps readiness
    // public Builder kubernetesComponentVersions(
    //    Map<String, Map<String, String>> kubernetesComponentVersions) {
    //  this.kubernetesComponentVersions = kubernetesComponentVersions;
    //  return this;
    // }

    public VersionScriptResult build() {
      return new VersionScriptResult(this);
    }
  }
}
