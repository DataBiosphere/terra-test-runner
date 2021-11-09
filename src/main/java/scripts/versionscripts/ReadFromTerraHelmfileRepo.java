package scripts.versionscripts;

import bio.terra.testrunner.common.HelmRelease;
import bio.terra.testrunner.common.HelmReleaseVersion;
import bio.terra.testrunner.common.utils.KubernetesClientUtils;
import bio.terra.testrunner.runner.VersionScript;
import bio.terra.testrunner.runner.VersionScriptResult;
import bio.terra.testrunner.runner.config.ServerSpecification;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.File;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    // The importComponentVersions API route will be provided by DevOps sometime in the future
    Map<String, Map<String, String>> kubernetesComponentVersions =
        !server.skipKubernetes ? KubernetesClientUtils.importComponentVersions() : null;

    // Pull versions from terra-helmfile
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    mapper.findAndRegisterModules();
    HelmRelease helmRelease = mapper.readValue(new File(baseFilePath), HelmRelease.class);
    HelmRelease helmOverride = mapper.readValue(new File(overrideFilePath), HelmRelease.class);
    merge(helmOverride.getReleases(), helmRelease.getReleases());
    logger.info(
        "Done merging Helm override values from {} into base values obtained from {}",
        overrideFilePath,
        baseFilePath);

    // TODO pull these from env vars or terra-helmfile, add more service versions if needed
    String wsmHelmAppVersion =
        helmRelease.getReleases().get("workspacemanager").getAppVersion().orElse("");
    String wsmHelmChartVersion =
        helmRelease.getReleases().get("workspacemanager").getChartVersion().orElse("");

    return new VersionScriptResult.Builder()
        .wsmHelmAppVersion(wsmHelmAppVersion)
        .wsmHelmChartVersion(wsmHelmChartVersion)
        .kubernetesComponentVersions(kubernetesComponentVersions)
        .build();
  }

  private Map<String, HelmReleaseVersion> merge(
      Map<String, HelmReleaseVersion> from, Map<String, HelmReleaseVersion> to) {
    from.forEach(
        (app, version) ->
            to.merge(
                app,
                version,
                (toVersion, fromVersion) ->
                    new HelmReleaseVersion(
                        fromVersion.getEnabled() != null && fromVersion.getEnabled().isPresent()
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
}