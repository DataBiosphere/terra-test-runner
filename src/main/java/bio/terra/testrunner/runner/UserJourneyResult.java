package bio.terra.testrunner.runner;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.apache.commons.lang.StringUtils;

@SuppressFBWarnings(
    value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD",
    justification = "This POJO class is used for easy serialization to JSON using Jackson.")
public class UserJourneyResult {
  public String userJourneyDescription;

  public String threadName;

  public boolean completed;
  public long elapsedTimeNS;

  public boolean exceptionWasThrown;
  public String exceptionStackTrace;
  public String exceptionMessage;
  public int retryAttempts = 0;

  public UserJourneyResult() {}

  public UserJourneyResult(String userJourneyDescription, String threadName) {
    this.userJourneyDescription = userJourneyDescription;
    this.threadName = threadName;

    this.exceptionWasThrown = false;
    this.exceptionStackTrace = null;
    this.exceptionMessage = null;
    this.completed = false;
  }

  /**
   * Store the exception message and stack trace for the test results. Don't store the full {@link
   * Throwable} object, because that may not always be serializable. This class is serialized to
   * disk as part of writing out the test results, so it needs to be a POJO.
   */
  public void saveExceptionThrown(Throwable exceptionThrown) {
    exceptionWasThrown = true;
    exceptionMessage =
        StringUtils.isBlank(exceptionMessage)
            ? exceptionThrown.getMessage()
            : String.format("%s%n%s", exceptionMessage, exceptionThrown.getMessage());

    StringWriter stackTraceStr = new StringWriter();
    exceptionThrown.printStackTrace(new PrintWriter(stackTraceStr));
    exceptionStackTrace = stackTraceStr.toString();

    exceptionThrown.printStackTrace(); // print the stack trace to the console
  }
}
