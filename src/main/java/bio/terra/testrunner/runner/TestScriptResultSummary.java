package bio.terra.testrunner.runner;

import bio.terra.testrunner.common.BasicStatistics;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/** Summary statistics are pulled out into a separate class for easier summary reporting. */
@SuppressFBWarnings(
    value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD",
    justification = "This POJO class is used for easy serialization to JSON using Jackson.")
public class TestScriptResultSummary {
  public String testScriptName;
  public String testScriptDescription;

  public BasicStatistics elapsedTimeStatistics;

  public int totalRun; // total number of user journey threads submitted to the thread pool
  public int numCompleted; // number of user journey threads that completed
  public int numExceptionsThrown; // number of user journey threads that threw exceptions

  public boolean isFailure; // numCompleted < totalRun

  public TestScriptResultSummary() {} // default constructor so Jackson can deserialize

  TestScriptResultSummary(String testScriptName, String testScriptDescription) {
    this.testScriptName = testScriptName;
    this.testScriptDescription = testScriptDescription;
  }
}
