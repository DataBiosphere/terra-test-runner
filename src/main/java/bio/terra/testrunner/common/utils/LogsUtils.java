package bio.terra.testrunner.common.utils;

import bio.terra.testrunner.runner.config.ServerSpecification;
import bio.terra.testrunner.runner.config.ServiceAccountSpecification;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.logging.v2.LoggingClient;
import com.google.cloud.logging.v2.LoggingSettings;
import com.google.logging.v2.ListLogEntriesRequest;
import com.google.logging.v2.ProjectName;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogsUtils {
  private static final Logger logger = LoggerFactory.getLogger(LogsUtils.class);

  public static final List<String> loggingReadScope =
      Collections.unmodifiableList(Arrays.asList("https://www.googleapis.com/auth/logging.read"));

  // The log/metric timestamps are not exact and so trying to limit results to too small a window
  // might be misleading. This parameter sets the minimum time interval size. If the interval
  // specified is less than this, then the code will expand the interval to the minimum size.
  private static int minimumTimeRangeSizeInSeconds = 1;

  private LogsUtils() {}

  /**
   * Build a Google Logging client object with credentials for a given service account. The client
   * object is newly created on each call to this method; it is not cached.
   */
  public static LoggingClient getClientForServiceAccount(ServiceAccountSpecification serviceAccount)
      throws Exception {
    GoogleCredentials serviceAccountCredentials =
        AuthenticationUtils.getServiceAccountCredential(serviceAccount, loggingReadScope);
    LoggingSettings loggingServiceSettings =
        LoggingSettings.newBuilder()
            .setCredentialsProvider(FixedCredentialsProvider.create(serviceAccountCredentials))
            .build();
    LoggingClient loggingServiceClient = LoggingClient.create(loggingServiceSettings);
    return loggingServiceClient;
  }

  /** Request the raw logging data points. */
  public static LoggingClient.ListLogEntriesPagedResponse requestLogEntries(
      LoggingClient loggingServiceClient, ProjectName project, String filter, String pageToken)
      throws Exception {
    // Page<LogEntry> entries = loggingClient.listLogEntries(
    // Logging.EntryListOption.filter(filter)); // v1 client api

    ListLogEntriesRequest.Builder requestBuilder =
        ListLogEntriesRequest.newBuilder().addResourceNames(project.toString()).setFilter(filter);
    if (pageToken != null) {
      requestBuilder = requestBuilder.setPageToken(pageToken);
    }
    ListLogEntriesRequest request = requestBuilder.build();
    LoggingClient.ListLogEntriesPagedResponse response =
        loggingServiceClient.listLogEntries(request);

    return response;
  }

  /** Build a logging filter on the container name and namespace */
  public static String buildStdoutContainerAndNamespaceFilter(
      ServerSpecification server, long startTimeMS, long endTimeMS) {
    // test run should set the start/end time before/after the user journey threads run
    if (startTimeMS == -1 || endTimeMS == -1) {
      throw new RuntimeException("Start/end time was not set for this test run.");
    }

    // make sure interval is at least __ seconds long
    long minIntervalMS = minimumTimeRangeSizeInSeconds * 1000;
    if (endTimeMS - startTimeMS < minIntervalMS) {
      logger.info(
          "Test run lasted less than {} seconds. Expanding logs interval to include the {} seconds before the test run start.",
          minimumTimeRangeSizeInSeconds,
          minimumTimeRangeSizeInSeconds);
      startTimeMS -= minIntervalMS;
    }

    // convert start/end time from milliseconds to ISO-8601 format timestamp
    startTimeMS -= 1000; // round down a second
    endTimeMS += 1000; // round up a second
    DateFormat dateFormat =
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS'Z'"); // Quoted Z to indicate UTC
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    String startTimestamp = dateFormat.format(new Date(startTimeMS));
    String endTimestamp = dateFormat.format(new Date(endTimeMS));
    logger.info("startTimestamp: {}, endTimestamp: {}", startTimestamp, endTimestamp);

    return "logName="
        + ProjectName.of(server.cluster.project).toString()
        + "/logs/stdout"
        + " AND resource.type=\"k8s_container\""
        + " AND resource.labels.project_id=\""
        + server.cluster.project
        + "\""
        + " AND resource.labels.location=\""
        + server.cluster.region
        + "\""
        + " AND resource.labels.cluster_name=\""
        + server.cluster.clusterShortName
        + "\""
        + " AND resource.labels.namespace_name=\""
        + server.cluster.namespace
        + "\""
        + " AND labels.k8s-pod/component=\""
        + server.cluster.containerName
        + "\""
        + " AND timestamp>=\""
        + startTimestamp
        + "\""
        + " AND timestamp<=\""
        + endTimestamp
        + "\"";
  }

  /** Build a pretty description string from a map of key:value parameter pairs. */
  public static String parametersToString(Map<String, String> params) {
    StringBuilder sb = new StringBuilder();
    if (params == null || params.keySet().isEmpty()) return "{}";
    for (Map.Entry<String, String> param : params.entrySet()) {
      sb.append("{").append(param.getKey()).append(": ").append(param.getValue()).append("},");
    }
    return sb.toString();
  }
}
