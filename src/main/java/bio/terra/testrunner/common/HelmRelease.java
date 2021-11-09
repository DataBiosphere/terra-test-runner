package bio.terra.testrunner.common;

import java.util.Map;

public class HelmRelease {
  private Map<String, HelmReleaseVersion> releases;

  public Map<String, HelmReleaseVersion> getReleases() {
    return releases;
  }

  public void setReleases(Map<String, HelmReleaseVersion> releases) {
    this.releases = releases;
  }
}
