package bio.terra.testrunner.uploader;

import bio.terra.testrunner.runner.config.ServiceAccountSpecification;
import java.nio.file.Path;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class UploadScript {
  private static final Logger logger = LoggerFactory.getLogger(UploadScript.class);

  /** Public constructor so that this class can be instantiated via reflection. */
  public UploadScript() {}

  /**
   * Setter for any parameters required by the upload script. These parameters will be set by the
   * Result Uploader based on the current Upload List, and can be used by the upload script methods.
   *
   * @param parameters map of string key-value pairs supplied by the upload list
   */
  public void setParameters(Map<String, String> parameters) throws Exception {}

  /**
   * Upload the test results saved to the given directory. Results may include Test Runner
   * client-side output and any relevant measurements collected.
   *
   * @param outputDirectory the output directory where the test results are saved
   * @param uploaderServiceAccount the service account to use for the upload
   */
  public void uploadResults(
      Path outputDirectory, ServiceAccountSpecification uploaderServiceAccount) throws Exception {
    throw new UnsupportedOperationException("uploadResults must be overridden by sub-classes");
  }
}
