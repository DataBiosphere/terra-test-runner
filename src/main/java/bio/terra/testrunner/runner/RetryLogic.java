package bio.terra.testrunner.runner;

import java.util.concurrent.TimeUnit;

public class RetryLogic {
  private int maxRetries;
  private int retryAttempts;
  private long timeToWait;

  public RetryLogic(int maxRetries, long timeToWait) {
    this.maxRetries = maxRetries;
    this.retryAttempts = 0;
    this.timeToWait = timeToWait;
  }

  public void retry(RetryImpl retryImpl) throws Exception {
    if (shouldRetry()) {
      retryAttempts++;
      retryImpl.run();
      waitForNextRetry();
    } else {
      throw new Exception(String.format("Failed after %s retry attempt(s).", retryAttempts));
    }
  }

  public boolean shouldRetry() {
    return retryAttempts < maxRetries;
  }

  public void waitForNextRetry() throws Exception {
    TimeUnit.MILLISECONDS.sleep(timeToWait);
  }

  @FunctionalInterface
  interface RetryImpl {
    Boolean run() throws Exception;
  }
}
