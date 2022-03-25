package scripts.versionscripts;

import bio.terra.testrunner.runner.VersionScript;
import bio.terra.testrunner.runner.VersionScriptResult;
import bio.terra.testrunner.runner.config.ServerSpecification;
import bio.terra.testrunner.runner.version.GitVersion;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scripts.versionscripts.model.GitRelease;

public class ReadFromGitCommitLog extends VersionScript {
  private static final Logger logger = LoggerFactory.getLogger(ReadFromGitCommitLog.class);

  public static final String GIT_DIR_PARAMETER_KEY = "git-dir";

  /** Public constructor so that this class can be instantiated via reflection. */
  public ReadFromGitCommitLog() {}

  private String gitDir;

  /**
   * Setter for any parameters required by the version script. These parameters will be set by the
   * Test Runner based on the current Test Configuration, and can be used by the version script
   * methods.
   *
   * @param parametersMap map of string key-value pairs supplied by the server.versionScripts
   *     configuration
   */
  @Override
  public void setParametersMap(Map<String, String> parametersMap) throws Exception {
    if (parametersMap == null || !parametersMap.containsKey(GIT_DIR_PARAMETER_KEY)) {
      throw new IllegalArgumentException(
          "Must provide file path of local .git as git-dir in the parameters list");
    }
    gitDir = parametersMap.get(GIT_DIR_PARAMETER_KEY);
  }

  /** This method determines the version by reading the versions from Git commit log. */
  public VersionScriptResult determineVersion(ServerSpecification server) throws Exception {
    return new VersionScriptResult.Builder().gitVersions(buildGitVersion(server, gitDir)).build();
  }

  public static List<GitVersion> buildGitVersion(ServerSpecification server, String gitDir)
      throws Exception {

    GitRelease git = new GitRelease(gitDir);

    return Arrays.asList(
        new GitVersion.Builder()
            .remoteOriginUrl(git.getRemoteOriginUrl())
            .branch(git.getBranch())
            .refHeadCommit(git.getRefHeadCommit())
            .shortRefHeadCommit(git.getShortRefHeadCommit())
            .build());
  }
}
