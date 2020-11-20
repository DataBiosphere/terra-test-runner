package bio.terra.testrunner.plugin;

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
