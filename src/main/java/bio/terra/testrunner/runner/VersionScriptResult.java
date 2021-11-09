package bio.terra.testrunner.runner;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;

@SuppressFBWarnings(
    value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD",
    justification = "This POJO class is used for easy serialization to JSON using Jackson.")
public class VersionScriptResult {
  // TODO: add more fields here for other version information we may care about
  //  (e.g. git hashes, helm versions for other services)
  public String wsmHelmAppVersion;
  public String wsmHelmChartVersion;

  // component version from a Kubernetes ConfigMap
  public Map<String, Map<String, String>> kubernetesComponentVersions;

  public VersionScriptResult(Builder builder) {
    this.wsmHelmAppVersion = builder.wsmHelmAppVersion;
    this.wsmHelmChartVersion = builder.wsmHelmChartVersion;
    this.kubernetesComponentVersions = builder.kubernetesComponentVersions;
  }

  public static class Builder {
    private String wsmHelmAppVersion;
    private String wsmHelmChartVersion;
    private Map<String, Map<String, String>> kubernetesComponentVersions;

    public Builder wsmHelmAppVersion(String wsmHelmAppVersion) {
      this.wsmHelmAppVersion = wsmHelmAppVersion;
      return this;
    }

    public Builder wsmHelmChartVersion(String wsmHelmChartVersion) {
      this.wsmHelmChartVersion = wsmHelmChartVersion;
      return this;
    }

    public Builder kubernetesComponentVersions(
        Map<String, Map<String, String>> kubernetesComponentVersions) {
      this.kubernetesComponentVersions = kubernetesComponentVersions;
      return this;
    }

    public VersionScriptResult build() {
      return new VersionScriptResult(this);
    }
  }
}