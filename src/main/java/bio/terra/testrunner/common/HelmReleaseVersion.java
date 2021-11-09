package bio.terra.testrunner.common;

import java.util.Optional;

public class HelmReleaseVersion {
  private Optional<Boolean> enabled;
  private Optional<String> chartVersion;
  private Optional<String> appVersion;

  public HelmReleaseVersion() {}

  public HelmReleaseVersion(
      Optional<Boolean> enabled, Optional<String> chartVersion, Optional<String> appVersion) {
    this.enabled = enabled;
    this.chartVersion = chartVersion;
    this.appVersion = appVersion;
  }

  public Optional<Boolean> getEnabled() {
    return enabled;
  }

  public void setEnabled(Optional<Boolean> enabled) {
    this.enabled = enabled;
  }

  public Optional<String> getChartVersion() {
    return chartVersion;
  }

  public void setChartVersion(Optional<String> chartVersion) {
    this.chartVersion = chartVersion;
  }

  public Optional<String> getAppVersion() {
    return appVersion;
  }

  public void setAppVersion(Optional<String> appVersion) {
    this.appVersion = appVersion;
  }
}