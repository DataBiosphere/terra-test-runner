package scripts.versionscripts.model;

import java.io.File;
import java.io.IOException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.slf4j.LoggerFactory;

public final class GitRelease {
  private String gitDir;
  private Repository repository;

  /**
   * @param gitDir is the local .git directory where config is located
   */
  public GitRelease(String gitDir) {
    this.gitDir = gitDir;
  }

  /**
   * @return a Repository object representing the git config in gitDir
   * @throws IOException
   */
  public Repository getRepository() throws IOException {
    if (repository == null) {
      // Disable DEBUG logging for org.eclipse.jgit logger
      ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger("org.eclipse.jgit"))
          .setLevel(ch.qos.logback.classic.Level.toLevel("info"));

      repository = new RepositoryBuilder().setGitDir(new File(gitDir)).build();
    }
    return repository;
  }

  /**
   * @return remote origin url of current git config
   * @throws IOException
   */
  public String getRemoteOriginUrl() throws IOException {
    return getRepository().getConfig().getString("remote", "origin", "url");
  }

  /**
   * @return branch of current git config
   * @throws IOException
   */
  public String getBranch() throws IOException {
    return getRepository().getBranch();
  }

  /**
   * @return HEAD commit hash
   * @throws IOException
   */
  public String getRefHeadCommit() throws IOException {
    return getRepository().resolve("HEAD").getName();
  }

  /**
   * @return short HEAD commit hash
   * @throws IOException
   */
  public String getShortRefHeadCommit() throws IOException {
    return getRepository().resolve("HEAD").abbreviate(7).name();
  }
}
