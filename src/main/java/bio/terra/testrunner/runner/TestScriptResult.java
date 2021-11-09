package bio.terra.testrunner.runner;

import bio.terra.testrunner.common.BasicStatistics;
import bio.terra.testrunner.runner.config.TestScriptSpecification;
import java.util.List;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

public class TestScriptResult {
  public List<UserJourneyResult> userJourneyResults;
  public TestScriptResultSummary summary;

  public TestScriptResult(
      TestScriptSpecification testScriptSpecification, List<UserJourneyResult> userJourneyResults) {
    this.userJourneyResults = userJourneyResults;

    summary =
        new TestScriptResultSummary(
            testScriptSpecification.name, testScriptSpecification.description);
    calculateStatistics();
  }

  public TestScriptResultSummary getSummary() {
    return summary;
  }

  /** Loop through the UserJourneyResults calculating reporting statistics of interest. */
  private void calculateStatistics() {
    DescriptiveStatistics descriptiveStatistics = new DescriptiveStatistics();
    for (int ctr = 0; ctr < userJourneyResults.size(); ctr++) {
      UserJourneyResult result = userJourneyResults.get(ctr);

      // count the number of user journeys that completed and threw exceptions
      summary.numCompleted += (result.completed) ? 1 : 0;
      summary.numExceptionsThrown += result.exceptionWasThrown ? 1 : 0;

      // convert elapsed time from nanosecods to milliseconds
      descriptiveStatistics.addValue(result.elapsedTimeNS / (1e6));
    }
    summary.elapsedTimeStatistics =
        BasicStatistics.calculateStandardStatistics(descriptiveStatistics);
    summary.totalRun = userJourneyResults.size();

    summary.isFailure =
        (summary.numCompleted < summary.totalRun) || (summary.numExceptionsThrown > 0);
  }
}
