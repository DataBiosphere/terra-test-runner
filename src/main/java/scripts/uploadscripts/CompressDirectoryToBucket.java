package scripts.uploadscripts;

import bio.terra.testrunner.common.utils.FileUtils;
import bio.terra.testrunner.common.utils.StorageUtils;
import bio.terra.testrunner.runner.TestRunSummary;
import bio.terra.testrunner.runner.TestRunner;
import bio.terra.testrunner.runner.config.ServiceAccountSpecification;
import bio.terra.testrunner.uploader.UploadScript;
import com.google.api.client.util.ByteStreams;
import com.google.cloud.WriteChannel;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.file.Path;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompressDirectoryToBucket extends UploadScript {
  private static final Logger logger = LoggerFactory.getLogger(CompressDirectoryToBucket.class);

  public static final String BUCKET_PATH_PARAMETER_KEY = "bucket-path";

  /** Public constructor so that this class can be instantiated via reflection. */
  public CompressDirectoryToBucket() {}

  protected String bucketPath;

  /**
   * Setter for any parameters required by the upload script. These parameters will be set by the
   * Result Uploader based on the current Upload List, and can be used by the upload script methods.
   *
   * @param parametersMap lmap of string key-value pairs supplied by the upload list
   */
  public void setParametersMap(Map<String, String> parametersMap) throws Exception {
    if (parametersMap == null || !parametersMap.containsKey(BUCKET_PATH_PARAMETER_KEY)) {
      throw new IllegalArgumentException("Must provide bucket-path in the parameters list");
    }
    bucketPath = parametersMap.get(BUCKET_PATH_PARAMETER_KEY);
    if (!bucketPath.startsWith("gs://")) { // only handle GCS buckets
      throw new IllegalArgumentException("Bucket path must start with gs://");
    }
  }

  /**
   * Upload the test results saved to the given directory. Results may include Test Runner
   * client-side output and any relevant measurements collected.
   */
  @SuppressFBWarnings(
      value = {"RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE"},
      justification =
          "Spotbugs fails with a redundant null check exception in the nested try-with-resources. More details here: https://github.com/spotbugs/spotbugs/issues/600")
  public void uploadResults(
      Path outputDirectory, ServiceAccountSpecification uploaderServiceAccount) throws Exception {
    // archive file path will be: outputDirectory parent directory + test run id + .tar.gz
    Path outputDirectoryParent = outputDirectory.getParent();
    if (outputDirectoryParent == null) {
      throw new IllegalArgumentException(
          "Parent directory of the directory to compress is null: "
              + outputDirectory.toAbsolutePath());
    }
    TestRunSummary testRunSummary = TestRunner.getTestRunSummary(outputDirectory);
    String archiveFileName = testRunSummary.id + ".tar.gz";
    Path archiveFile = outputDirectoryParent.resolve(archiveFileName);

    // create the archive file locally
    FileUtils.compressDirectory(outputDirectory, archiveFile);
    logger.info("Compressed directory written locally: {}", archiveFile.toAbsolutePath());

    // upload the archive file to a bucket
    Storage storageClient = StorageUtils.getClientForServiceAccount(uploaderServiceAccount);
    BlobInfo blobInfo =
        BlobInfo.newBuilder(bucketPath.replace("gs://", ""), archiveFileName)
            .setContentType("application/gzip")
            .build();
    try (WriteChannel writer = storageClient.writer(blobInfo)) {
      try (InputStream inputStream = new FileInputStream(archiveFile.toFile())) {
        ByteStreams.copy(inputStream, Channels.newOutputStream(writer));
      }
    }
    logger.info(
        "Compressed directory written to bucket: {}/{}", blobInfo.getBucket(), blobInfo.getName());
  }
}
