package scripts.versionscripts;

import bio.terra.testrunner.runner.VersionScript;
import bio.terra.testrunner.runner.VersionScriptResult;
import bio.terra.testrunner.runner.config.ServerSpecification;
import java.util.List;
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
   * This method determines the version by reading env vars set to the helm app/chart versions in
   * the terra-helmfile GitHub repository.
   */
  public VersionScriptResult determineVersion(ServerSpecification server) throws Exception {
    // TODO pull these from env vars, add more service versions if needed
    String wsmHelmAppVersion = "1.0";
    String wsmHelmChartVersion = "2.0";

    return new VersionScriptResult.Builder()
        .wsmHelmAppVersion(wsmHelmAppVersion)
        .wsmHelmChartVersion(wsmHelmChartVersion)
        .build();
  }
}
