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
public class TestRunFullOutput extends TestRunSummary {
  @JsonView(SummaryViews.FullOutput.class)
  public TestConfiguration testConfiguration;

  @JsonView(SummaryViews.FullOutput.class)
  public List<TestScriptResult> testScriptResults;

  @JsonView(SummaryViews.FullOutput.class)
  public List<TerraVersion> terraVersions;

  public TestRunFullOutput() {
    super();
  }

  public TestRunFullOutput(String id) {
    super();
    this.id = id;
  }
}
