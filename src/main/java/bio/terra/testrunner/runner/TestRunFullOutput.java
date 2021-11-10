package bio.terra.testrunner.runner;

import bio.terra.testrunner.runner.config.TestConfiguration;
import com.fasterxml.jackson.annotation.JsonView;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;

/**
 * A subclass of TestRunSummary with additional properties testConfig,
 * userJourneySnapshotsCollection, terraVersions.
 *
 * <p>This extended version is suitable for Cloud Function ingestion of test results as single rows
 * and for streaming inserts to BigQuery in bulk.
 */
/**
 * All output information from a test run is included in this POJO for easier automated processing
 * (i.e. one file instead of four).
 */
@SuppressFBWarnings(
    value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD",
    justification = "This POJO class is used for easy serialization to JSON using Jackson.")
public class TestRunFullOutput extends TestRunSummary {
  @JsonView(SummaryViews.FullOutput.class)
  public TestConfiguration testConfiguration;

  @JsonView(SummaryViews.FullOutput.class)
  public List<TestScriptResult> testScriptResults;

  @JsonView(SummaryViews.FullOutput.class)
  public VersionScriptResult terraVersions;

  public TestRunFullOutput() {
    super();
  }

  public TestRunFullOutput(String id) {
    super();
    this.id = id;
  }
}
