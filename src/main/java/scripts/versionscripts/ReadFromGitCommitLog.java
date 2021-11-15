package scripts.versionscripts;

import bio.terra.testrunner.runner.VersionScript;
import bio.terra.testrunner.runner.VersionScriptResult;
import bio.terra.testrunner.runner.config.ServerSpecification;
import bio.terra.testrunner.runner.version.GitVersion;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scripts.versionscripts.model.GitRelease;

public class ReadFromGitCommitLog extends VersionScript {
  private static final Logger logger = LoggerFactory.getLogger(ReadFromGitCommitLog.class);

  /** Public constructor so that this class can be instantiated via reflection. */
  public ReadFromGitCommitLog() {}

  private String gitDir;

  /**
   * Setter for any parameters required by the version script. These parameters will be set by the
   * Test Runner based on the current Test Configuration, and can be used by the version script
   * methods.
   *
   * @param parameters list of string parameters supplied by the version list
   */
  public void setParameters(List<String> parameters) throws Exception {
    if (parameters == null || parameters.size() < 1) {
      throw new IllegalArgumentException(
          "Must provide file path of local .git in the parameters list");
    }
    gitDir = parameters.get(0);
  }

  /**
   * This method determines the version by reading the helm app/chart versions in the terra-helmfile
   * GitHub repository.
   */
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
