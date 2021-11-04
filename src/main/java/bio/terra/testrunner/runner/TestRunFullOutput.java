package bio.terra.testrunner.runner;

import bio.terra.testrunner.runner.config.TestConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;

/**
 * All output information from a test run is included in this POJO for easier automated processing
 * (i.e. one file instead of four).
 */
@SuppressFBWarnings(
    value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD",
    justification = "This POJO class is used for easy serialization to JSON using Jackson.")
public class TestRunFullOutput {
  public TestConfiguration testConfiguration;
  public List<TestScriptResult> testScriptResults;
  public TestRunSummary testRunSummary;
  public Map<String, Map<String, String>> componentVersions;

  public TestRunFullOutput(
      TestConfiguration testConfiguration,
      List<TestScriptResult> testScriptResults,
      TestRunSummary testRunSummary,
      Map<String, Map<String, String>> componentVersions) {
    this.testConfiguration = testConfiguration;
    this.testScriptResults = testScriptResults;
    this.testRunSummary = testRunSummary;
    this.componentVersions = componentVersions;
  }
}
