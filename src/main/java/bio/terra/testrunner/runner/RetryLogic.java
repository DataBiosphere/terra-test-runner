package bio.terra.testrunner.runner;

import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RetryLogic {
  private static final Logger logger = LoggerFactory.getLogger(RetryLogic.class);

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
      logger.info("Retry attempt {}.", retryAttempts);
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
    logger.info("Sleeping for {} milliseconds before exit or next retry.", timeToWait);
    TimeUnit.MILLISECONDS.sleep(timeToWait);
  }

  @FunctionalInterface
  interface RetryImpl {
    void run() throws Exception;
  }
}
