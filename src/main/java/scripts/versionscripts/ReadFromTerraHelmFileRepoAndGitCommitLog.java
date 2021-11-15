package scripts.versionscripts;

import bio.terra.testrunner.runner.VersionScript;
import bio.terra.testrunner.runner.VersionScriptResult;
import bio.terra.testrunner.runner.config.ServerSpecification;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReadFromTerraHelmFileRepoAndGitCommitLog extends VersionScript {
  private static final Logger logger =
      LoggerFactory.getLogger(ReadFromTerraHelmFileRepoAndGitCommitLog.class);

  /** Public constructor so that this class can be instantiated via reflection. */
  public ReadFromTerraHelmFileRepoAndGitCommitLog() {}

  private String appName;
  private String baseFilePath;
  private String overrideFilePath;
  private String gitDir;

  /**
   * Setter for any parameters required by the version script. These parameters will be set by the
   * Test Runner based on the current Test Configuration, and can be used by the version script
   * methods.
   *
   * @param parameters list of string parameters supplied by the version list
   */
  public void setParameters(List<String> parameters) throws Exception {
    if (parameters == null || parameters.size() < 4) {
      throw new IllegalArgumentException(
          "Must provide terra helmfile file paths in the parameters list");
    }
    appName = parameters.get(0);
    baseFilePath = parameters.get(1);
    overrideFilePath = parameters.get(2);
    gitDir = parameters.get(3);
  }

  /**
   * This method determines the version from following sources: helm app/chart versions in the
   * terra-helmfile GitHub repository and Git Commit Log
   */
  public VersionScriptResult determineVersion(ServerSpecification server) throws Exception {
    return new VersionScriptResult.Builder()
        .helmVersions(
            ReadFromTerraHelmfileRepo.buildHelmVersion(
                server, appName, baseFilePath, overrideFilePath))
        .gitVersions(ReadFromGitCommitLog.buildGitVersion(server, gitDir))
        .build();
  }
}
