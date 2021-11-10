package bio.terra.testrunner.runner;

import bio.terra.testrunner.runner.config.ServerSpecification;
import java.util.List;

public abstract class VersionScript {
  /** Public constructor so that this class can be instantiated via reflection. */
  public VersionScript() {}

  /**
   * Setter for any parameters required by the version script. These parameters will be set by the
   * Test Runner based on the current Test Configuration, and can be used by the Version script
   * methods.
   *
   * @param parameters list of string parameters supplied by the test configuration
   */
  public void setParameters(List<String> parameters) throws Exception {}

  /** The version script determineVersion method looks up the version. */
  public List<VersionScriptResult> determineVersion(ServerSpecification server) throws Exception {
    throw new UnsupportedOperationException("determineVersion must be overridden by sub-classes");
  }
}
