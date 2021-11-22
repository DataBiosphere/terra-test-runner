package scripts.versionscripts.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/**
 * The HelmRelease class represents a map of all MCTerra releases as defined in terra-helmfile. For
 * example: https://github.com/broadinstitute/terra-helmfile/blob/master/versions/app/dev.yaml
 */
public final class HelmRelease {
  @JsonIgnore private Optional<String> defaultCluster;
  private Map<String, HelmReleaseVersion> releases;

  public Map<String, HelmReleaseVersion> getReleases() {
    return releases;
  }

  public void setReleases(Map<String, HelmReleaseVersion> releases) {
    this.releases = releases;
  }

  public Optional<String> getDefaultCluster() {
    return defaultCluster;
  }

  public void setDefaultCluster(Optional<String> defaultCluster) {
    this.defaultCluster = defaultCluster;
  }

  /**
   * This method parses a version file in yaml format and returns the serialized HelmRelease object.
   *
   * @param path of version file
   * @return HelmRelease object
   * @throws IOException
   */
  public static HelmRelease fromFile(String path) throws IOException {
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    mapper.findAndRegisterModules();
    return mapper.readValue(new File(path), HelmRelease.class);
  }

  /**
   * @param from a Java Map instance that represents the source of updates
   * @param to a Java Map instance to receive updates from source map
   * @return the same 'to' instance with merged keys from the source map
   */
  public static HelmRelease merge(HelmRelease from, HelmRelease to) {
    from.getReleases()
        .forEach(
            (app, version) ->
                to.getReleases()
                    .merge(
                        app,
                        version,
                        (toVersion, fromVersion) ->
                            new HelmRelease.HelmReleaseVersion(
                                fromVersion.getEnabled() != null
                                        && fromVersion.getEnabled().isPresent()
                                    ? fromVersion.getEnabled()
                                    : toVersion.getEnabled(),
                                fromVersion.getChartVersion() != null
                                        && fromVersion.getChartVersion().isPresent()
                                    ? fromVersion.getChartVersion()
                                    : toVersion.getChartVersion(),
                                fromVersion.getAppVersion() != null
                                        && fromVersion.getAppVersion().isPresent()
                                    ? fromVersion.getAppVersion()
                                    : toVersion.getAppVersion())));
    return to;
  }

  /**
   * The HelmReleaseVersion represents the version properties that terra-helmfile keeps track of per
   * MCTerra service. For example: 'duos' in
   * https://github.com/broadinstitute/terra-helmfile/blob/master/versions/app/dev.yaml and
   * https://github.com/broadinstitute/terra-helmfile/blob/master/environments/live/dev.yaml
   */
  public static class HelmReleaseVersion {
    private Optional<Boolean> enabled;
    private Optional<String> chartVersion;
    private Optional<String> appVersion;
    @JsonIgnore private Optional<String> cluster;

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

    public Optional<String> getCluster() {
      return cluster;
    }

    public void setCluster(Optional<String> cluster) {
      this.cluster = cluster;
    }
  }
}
