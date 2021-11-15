package bio.terra.testrunner.runner.version;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

// Builder for helm versions
@SuppressFBWarnings(
    value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD",
    justification = "This POJO class is used for easy serialization to JSON using Jackson.")
public class HelmVersion {
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
