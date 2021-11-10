package scripts.versionscripts.model;

import java.util.Map;

/**
 * The HelmRelease class represents a map of all MCTerra releases as defined in terra-helmfile. For
 * example: https://github.com/broadinstitute/terra-helmfile/blob/master/versions/app/dev.yaml
 */
public class HelmRelease {
  private Map<String, HelmReleaseVersion> releases;

  public Map<String, HelmReleaseVersion> getReleases() {
    return releases;
  }

  public void setReleases(Map<String, HelmReleaseVersion> releases) {
    this.releases = releases;
  }
}
