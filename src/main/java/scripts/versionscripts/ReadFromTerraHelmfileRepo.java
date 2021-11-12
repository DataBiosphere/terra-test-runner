package scripts.versionscripts;

import bio.terra.testrunner.runner.VersionScript;
import bio.terra.testrunner.runner.VersionScriptResult;
import bio.terra.testrunner.runner.config.ServerSpecification;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scripts.versionscripts.model.HelmRelease;

public class ReadFromTerraHelmfileRepo extends VersionScript {
  private static final Logger logger = LoggerFactory.getLogger(ReadFromTerraHelmfileRepo.class);

  /** Public constructor so that this class can be instantiated via reflection. */
  public ReadFromTerraHelmfileRepo() {}

  private String baseFilePath;
  private String overrideFilePath;

  /**
   * Setter for any parameters required by the version script. These parameters will be set by the
   * Test Runner based on the current Test Configuration, and can be used by the version script
   * methods.
   *
   * @param parameters list of string parameters supplied by the version list
   */
  public void setParameters(List<String> parameters) throws Exception {
    if (parameters == null || parameters.size() < 2) {
      throw new IllegalArgumentException(
          "Must provide terra helmfile file paths in the parameters list");
    }
    baseFilePath = parameters.get(0);
    overrideFilePath = parameters.get(1);
  }

  /**
   * This method determines the version by reading the helm app/chart versions in the terra-helmfile
   * GitHub repository.
   */
  public VersionScriptResult determineVersion(ServerSpecification server) throws Exception {
    return new VersionScriptResult.Builder()
        .helmVersions(buildHelmVersion(server, baseFilePath, overrideFilePath))
        .build();
  }

  public static List<VersionScriptResult.HelmVersion> buildHelmVersion(
      ServerSpecification server, String baseFilePath, String overrideFilePath) throws Exception {
    // Pull versions from terra-helmfile
    HelmRelease helmRelease = HelmRelease.fromFile(baseFilePath);
    HelmRelease helmOverride = HelmRelease.fromFile(overrideFilePath);

    // Loop through the override helm values and replacing any base helm values, then returning the
    // now-modified base helm values map.
    HelmRelease.merge(helmOverride, helmRelease);
    logger.info(
        "Done merging Helm override values from {} into base values obtained from {}",
        overrideFilePath,
        baseFilePath);

    // Pull 'workspacemanager' versions from terra-helmfile, add more service versions if needed.
    String wsmHelmAppVersion =
        helmRelease.getReleases().get("workspacemanager").getAppVersion().orElse("");
    String wsmHelmChartVersion =
        helmRelease.getReleases().get("workspacemanager").getChartVersion().orElse("");

    return Arrays.asList(
        new VersionScriptResult.HelmVersion.Builder()
            .appName("workspacemanager")
            .helmAppVersion(wsmHelmAppVersion)
            .helmChartVersion(wsmHelmChartVersion)
            .build());
  }
}
