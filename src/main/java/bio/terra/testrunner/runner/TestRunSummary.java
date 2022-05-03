package bio.terra.testrunner.runner;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/** A base class with test runner summary statistics. Suitable for human inspection. */
public class TestRunSummary {
  public String id;
  public long startTime = -1;
  public long startUserJourneyTime = -1;
  public long endUserJourneyTime = -1;
  public long endTime = -1;
  public List<TestScriptResultSummary> testScriptResultSummaries;

  public TestRunSummary() {}

  public TestRunSummary(String id) {
    this.id = id;
  }

  private String startTimestamp;
  private String startUserJourneyTimestamp;
  private String endUserJourneyTimestamp;
  private String endTimestamp;

  // Include user-provided TestSuite name in the summary:
  // This can be used to facilitate grouping of test runner results on the dashboard.
  private String testSuiteName;

  // GitHub Workflow Runtime Context
  private String githubRunId;
  private String githubRepoHtmlUrl;

  public String getStartTimestamp() {
    return millisecondsToTimestampString(startTime);
  }

  public String getStartUserJourneyTimestamp() {
    return millisecondsToTimestampString(startUserJourneyTime);
  }

  public String getEndUserJourneyTimestamp() {
    return millisecondsToTimestampString(endUserJourneyTime);
  }

  public String getEndTimestamp() {
    return millisecondsToTimestampString(endTime);
  }

  public String getTestSuiteName() {
    return testSuiteName;
  }

  public void setTestSuiteName(String testSuiteName) {
    this.testSuiteName = testSuiteName;
  }

  public String getGithubRunId() {
    return githubRunId;
  }

  public void setGithubRunId(String githubRunId) {
    this.githubRunId = githubRunId;
  }

  public String getGithubRepoHtmlUrl() {
    return githubRepoHtmlUrl;
  }

  public void setGithubRepoHtmlUrl(String githubRepoHtmlUrl) {
    this.githubRepoHtmlUrl = githubRepoHtmlUrl;
  }

  private static String millisecondsToTimestampString(long milliseconds) {
    DateFormat dateFormat =
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS'Z'"); // Quoted Z to indicate UTC
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    return dateFormat.format(new Date(milliseconds));
  }
}
