package bio.terra.testrunner.runner;

import bio.terra.testrunner.runner.config.TestConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A subclass of TestRunSummary with additional properties testConfig,
 * userJourneySnapshotsCollection, terraVersions.
 *
 * <p>This extended version is suitable for Cloud Function ingestion of test results as single rows
 * and for streaming inserts to BigQuery in bulk.
 *
 * <p>All output information from a test run is included in this POJO for easier automated
 * processing (i.e. one file instead of four).
 */
@SuppressFBWarnings(
    value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD",
    justification = "This POJO class is used for easy serialization to JSON using Jackson.")
public class TestRunFullOutput {
  public TestConfiguration testConfiguration;

  // The testScriptResults and testScriptResultSummaries must have identical size.
  // For each element in testScriptResults, we can derive the testScriptName and
  // testScriptDescription properties from the corresponding positional element in
  // testScriptResultSummaries.
  public List<Map<String, Object>> testScriptResults;
  public List<TestScriptResultSummary> testScriptResultSummaries;

  public List<VersionScriptResult> versionScriptResults;
  public String id;
  public long startTime = -1;
  public long startUserJourneyTime = -1;
  public long endUserJourneyTime = -1;
  public long endTime = -1;

  public String startTimestamp;
  public String startUserJourneyTimestamp;
  public String endUserJourneyTimestamp;
  public String endTimestamp;

  // Include user-provided TestSuite name in the summary:
  // This can be used to facilitate grouping of test runner results on the dashboard.
  public String testSuiteName;

  // GitHub Workflow Runtime Context
  public String githubRunId;
  public String githubRepository;
  public String githubServerUrl;

  public TestRunFullOutput(
      TestConfiguration testConfiguration,
      List<TestScriptResult> testScriptResults,
      TestRunSummary testRunSummary,
      List<VersionScriptResult> versionScriptResults) {
    this.testConfiguration = testConfiguration;
    this.testScriptResults = unnestUserJourneyResults(testScriptResults);
    this.versionScriptResults = versionScriptResults;
    this.id = testRunSummary.id;
    this.startTime = testRunSummary.startTime;
    this.startUserJourneyTime = testRunSummary.startUserJourneyTime;
    this.endUserJourneyTime = testRunSummary.endUserJourneyTime;
    this.endTime = testRunSummary.endTime;
    this.testScriptResultSummaries = testRunSummary.testScriptResultSummaries;
    this.startTimestamp = testRunSummary.getStartTimestamp();
    this.startUserJourneyTimestamp = testRunSummary.getStartUserJourneyTimestamp();
    this.endUserJourneyTimestamp = testRunSummary.getEndUserJourneyTimestamp();
    this.endTimestamp = testRunSummary.getEndTimestamp();
    this.testSuiteName = testRunSummary.getTestSuiteName();
    this.githubRunId = testRunSummary.getGithubRunId();
    this.githubServerUrl = testRunSummary.getGithubServerUrl();
    this.githubRepository = testRunSummary.getGithubRepository();
  }

  // A mapper that unnest List<TestScriptResult> from TestScriptResult
  private List<Map<String, Object>> unnestUserJourneyResults(
      List<TestScriptResult> testScriptResults) {
    return testScriptResults.stream()
        .map(
            r -> {
              String testScriptName = r.getSummary().testScriptName;
              String testScriptDescription = r.getSummary().testScriptDescription;
              List<UserJourneyResult> userJourneyResults = r.getUserJourneyResults();

              return new HashMap<String, Object>() {
                {
                  put("testScriptName", testScriptName);
                  put("testScriptDescription", testScriptDescription);
                  put("userJourneyResults", userJourneyResults);
                }
              };
            })
        .collect(Collectors.toList());
  }
}
