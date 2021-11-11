package bio.terra.testrunner.runner;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;

@SuppressFBWarnings(
    value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD",
    justification = "This POJO class is used for easy serialization to JSON using Jackson.")
public class VersionScriptResult {
  // TODO: add more fields here for other version information we may care about
  //  (e.g. git hashes, helm versions for other services)
  public List<HelmVersion> helmVersions;

  public VersionScriptResult() {}

  // add method for helm versions
  // TODO: add more overloaded add methods for other version sources
  public VersionScriptResult add(HelmVersion helmVersion) {
    if (helmVersions == null) {
      helmVersions = new ArrayList<>();
    }
    helmVersions.add(helmVersion);
    return this;
  }

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

  // TODO: QA-1643 Re-enable or add builder for importComponentVersions API route pending DevOps
  // readiness
  // public Map<String, Map<String, String>> kubernetesComponentVersions;
}
