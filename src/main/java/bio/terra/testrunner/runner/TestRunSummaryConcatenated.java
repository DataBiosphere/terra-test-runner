package bio.terra.testrunner.runner;

import bio.terra.testrunner.common.TerraVersion;
import bio.terra.testrunner.runner.config.TestConfiguration;
import com.fasterxml.jackson.annotation.JsonView;
import java.util.List;

/**
 * A subclass of TestRunSummary with additional properties testConfig,
 * userJourneySnapshotsCollection, terraVersions.
 *
 * <p>This extended version is suitable for Cloud Function ingestion of test results as single rows
 * and for streaming inserts to BigQuery in bulk.
 */
public class TestRunSummaryConcatenated extends TestRunSummary {
  @JsonView(SummaryViews.ConcatenatedSummary.class)
  private TestConfiguration testConfig;

  @JsonView(SummaryViews.ConcatenatedSummary.class)
  private List<TestScriptResult.TestScriptUserJourneySnapshots> userJourneySnapshotsCollection;

  @JsonView(SummaryViews.ConcatenatedSummary.class)
  private List<TerraVersion> terraVersions;

  public TestRunSummaryConcatenated() {
    super();
  }

  public TestRunSummaryConcatenated(String id) {
    super();
    this.id = id;
  }

  public TestConfiguration getTestConfig() {
    return testConfig;
  }

  public void setTestConfig(TestConfiguration testConfig) {
    this.testConfig = testConfig;
  }

  public List<TestScriptResult.TestScriptUserJourneySnapshots> getUserJourneySnapshotsCollection() {
    return userJourneySnapshotsCollection;
  }

  public void setUserJourneySnapshotsCollection(
      List<TestScriptResult.TestScriptUserJourneySnapshots> userJourneySnapshotsCollection) {
    this.userJourneySnapshotsCollection = userJourneySnapshotsCollection;
  }

  public List<TerraVersion> getTerraVersions() {
    return terraVersions;
  }

  public void setTerraVersions(List<TerraVersion> terraVersions) {
    this.terraVersions = terraVersions;
  }
}
