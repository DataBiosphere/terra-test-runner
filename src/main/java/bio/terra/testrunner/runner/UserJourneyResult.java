package bio.terra.testrunner.runner;

public class UserJourneyResult {
  public String userJourneyDescription;

  public String threadName;

  public boolean completed;
  public long elapsedTimeNS;
  public Exception exceptionThrown;

  public UserJourneyResult(String userJourneyDescription, String threadName) {
    this.userJourneyDescription = userJourneyDescription;
    this.threadName = threadName;

    this.exceptionThrown = null;
    this.completed = false;
  }
}
