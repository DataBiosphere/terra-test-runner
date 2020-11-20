package bio.terra.testrunner.plugin;

/**
 * This class defines a set of properties that are relevant to >1 task. These values can be set in
 * the calling Gradle file and supplied to each task when it's called. This class is not a good
 * place to put task-specific variables (e.g. the config file for the runTest task).
 */
public class TestRunnerPluginExtension {
  public static final String EXTENSION_NAME = "testRunner";

  private String SAKeyDirectoryPath;

  public String getSAKeyDirectoryPath() {
    return SAKeyDirectoryPath;
  }

  public void setSAKeyDirectoryPath(String SAKeyDirectoryPath) {
    this.SAKeyDirectoryPath = SAKeyDirectoryPath;
  }
}
