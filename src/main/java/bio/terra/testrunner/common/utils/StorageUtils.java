package bio.terra.testrunner.common.utils;

import bio.terra.testrunner.runner.config.ServiceAccountSpecification;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StorageUtils {
  private static final Logger logger = LoggerFactory.getLogger(StorageUtils.class);

  public static final List<String> storageScope =
      Collections.unmodifiableList(
          Arrays.asList("https://www.googleapis.com/auth/devstorage.full_control"));

  private StorageUtils() {}

  /**
   * Build a Google Storage client object with credentials for the given service account. The client
   * object is newly created on each call to this method; it is not cached.
   */
  public static Storage getClientForServiceAccount(ServiceAccountSpecification serviceAccount)
      throws Exception {
    logger.debug(
        "Fetching credentials and building Storage client object for service account: {}",
        serviceAccount.name);

    GoogleCredentials serviceAccountCredentials =
        AuthenticationUtils.getServiceAccountCredential(serviceAccount, storageScope);
    StorageOptions storageOptions =
        StorageOptions.newBuilder().setCredentials(serviceAccountCredentials).build();
    Storage storageClient = storageOptions.getService();

    return storageClient;
  }

  /**
   * Write the contents of a byte array to a file in the given GCS bucket.
   *
   * @param byteArray the bytes to write
   * @param fileName the name of the file
   * @param bucketName the bucket to write to
   * @return the created BlobId
   */
  public static BlobId writeBytesToFile(
      Storage storageClient, String bucketName, String fileName, byte[] byteArray) {
    BlobInfo blobToCreate = BlobInfo.newBuilder(bucketName, fileName).build();
    Blob createdBlob = storageClient.create(blobToCreate, byteArray);

    return createdBlob.getBlobId();
  }

  /** Convert a given BlobId to a gs:// path. Does not check for existence. */
  public static String blobIdToGSPath(BlobId blobId) {
    return String.format("gs://%s/%s", blobId.getBucket(), blobId.getName());
  }

  /** Delete all files in the given list. */
  public static void deleteFiles(Storage storageClient, List<BlobId> files) {
    for (BlobId file : files) {
      Blob blob = storageClient.get(file);
      if (blob == null) {
        logger.debug(
            "Blob not found: bucket = {}, file name = {}", file.getBucket(), file.getName());
      } else {
        logger.debug(
            "Deleting blob: bucket = {}, file name = {}", file.getBucket(), file.getName());
        blob.delete();
      }
    }
  }
}
