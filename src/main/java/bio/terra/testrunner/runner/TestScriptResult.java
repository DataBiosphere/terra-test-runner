package bio.terra.testrunner.runner;

import bio.terra.testrunner.common.BasicStatistics;
import bio.terra.testrunner.runner.config.TestScriptSpecification;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

public class TestScriptResult {
  public TestScriptResultSummary summary;
  public TestScriptUserJourneySnapshots userJourneySnapshots;

  /**
   * Summary statistics are pulled out into a separate inner class for easier summary reporting.
   * This class does not include a reference to the full TestScriptSpecification or the list of
   * UserJourneyResults.
   */
  @SuppressFBWarnings(
      value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD",
      justification = "This POJO class is used for easy serialization to JSON using Jackson.")
  public static class TestScriptResultSummary {
    public String testScriptName;
    public String testScriptDescription;

    public BasicStatistics elapsedTimeStatistics;

    public int totalRun; // total number of user journey threads submitted to the thread pool
    public int numCompleted; // number of user journey threads that completed
    public int numExceptionsThrown; // number of user journey threads that threw exceptions

    public boolean isFailure; // numCompleted < totalRun

    public TestScriptResultSummary() {} // default constructor so Jackson can deserialize

    private TestScriptResultSummary(String testScriptName, String testScriptDescription) {
      this.testScriptName = testScriptName;
      this.testScriptDescription = testScriptDescription;
    }
  }

  /** Store snapshots of UserJourneyResults, mainly used for reporting. */
  @SuppressFBWarnings(
      value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD",
      justification = "This POJO class is used for easy serialization to JSON using Jackson.")
  public static class TestScriptUserJourneySnapshots {
    public String testScriptName;
    public String testScriptDescription;

    public List<UserJourneyResult> userJourneyResults;

    public TestScriptUserJourneySnapshots() {}

    private TestScriptUserJourneySnapshots(String testScriptName, String testScriptDescription) {
      this.testScriptName = testScriptName;
      this.testScriptDescription = testScriptDescription;
    }
  }

  public TestScriptResult(
      TestScriptSpecification testScriptSpecification, List<UserJourneyResult> userJourneyResults) {
    this.userJourneySnapshots =
        new TestScriptUserJourneySnapshots(
            testScriptSpecification.name, testScriptSpecification.description);
    userJourneySnapshots.userJourneyResults = userJourneyResults;

    summary =
        new TestScriptResultSummary(
            testScriptSpecification.name, testScriptSpecification.description);
    calculateStatistics();
  }

  public TestScriptResultSummary getSummary() {
    return summary;
  }

  public TestScriptUserJourneySnapshots getUserJourneySnapshots() {
    return userJourneySnapshots;
  }

  /** Loop through the UserJourneyResults calculating reporting statistics of interest. */
  private void calculateStatistics() {
    DescriptiveStatistics descriptiveStatistics = new DescriptiveStatistics();
    for (int ctr = 0; ctr < userJourneySnapshots.userJourneyResults.size(); ctr++) {
      UserJourneyResult result = userJourneySnapshots.userJourneyResults.get(ctr);

      // count the number of user journeys that completed and threw exceptions
      summary.numCompleted += (result.completed) ? 1 : 0;
      summary.numExceptionsThrown += result.exceptionWasThrown ? 1 : 0;

      // convert elapsed time from nanosecods to milliseconds
      descriptiveStatistics.addValue(result.elapsedTimeNS / (1e6));
    }
    summary.elapsedTimeStatistics =
        BasicStatistics.calculateStandardStatistics(descriptiveStatistics);
    summary.totalRun = userJourneySnapshots.userJourneyResults.size();

    summary.isFailure =
        (summary.numCompleted < summary.totalRun) || (summary.numExceptionsThrown > 0);
  }
}
