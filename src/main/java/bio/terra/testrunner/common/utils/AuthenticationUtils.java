package bio.terra.testrunner.common.utils;

import bio.terra.testrunner.runner.config.ServiceAccountSpecification;
import bio.terra.testrunner.runner.config.TestUserSpecification;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class AuthenticationUtils {
  private static volatile GoogleCredentials applicationDefaultCredential;
  private static volatile GoogleCredentials serviceAccountCredential;
  private static Map<String, GoogleCredentials> delegatedUserCredentials =
      new ConcurrentHashMap<>();

  private static final Object lockApplicationDefaultCredential = new Object();
  private static final Object lockServiceAccountCredential = new Object();

  private AuthenticationUtils() {}

  // the list of scopes we request from end users when they log in. this should always match exactly
  // what the UI requests, so our tests represent actual user behavior
  private static final List<String> userLoginScopes = Arrays.asList("openid", "email", "profile");

  // the list of "extra" scopes we request for the test users, so that we can access BigQuery and
  // Cloud Storage directly (e.g. to query the snapshot table, write a file to a scratch bucket)
  private static final List<String> directAccessScopes =
      Arrays.asList(
          "https://www.googleapis.com/auth/bigquery",
          "https://www.googleapis.com/auth/devstorage.full_control");

  // cloud platform scope
  private static final String cloudPlatformScope = "https://www.googleapis.com/auth/cloud-platform";

  /**
   * Build a domain-wide delegated user credential with ("openid", "email", "profile") scopes.
   *
   * <p>This requires the email of the user, and a service account that has permissions to get
   * domain-wide delegated user credentials. My understanding is that domain-wide delegation is
   * GSuite-related, not GCP-related, and also not currently Terraform-able.
   *
   * <p>This credential is used, for example, with the Harry Potter users.
   *
   * @param testUserSpecification
   * @return a domain-wide delegated user credential
   */
  public static GoogleCredentials getDelegatedUserCredential(
      TestUserSpecification testUserSpecification) throws IOException {
    GoogleCredentials delegatedUserCredential =
        delegatedUserCredentials.get(testUserSpecification.userEmail);
    if (delegatedUserCredential != null) {
      return delegatedUserCredential;
    }

    List<String> scopes = new ArrayList<>();
    scopes.addAll(userLoginScopes);
    scopes.addAll(directAccessScopes);

    GoogleCredentials serviceAccountCredential =
        getServiceAccountCredential(testUserSpecification.delegatorServiceAccount);
    delegatedUserCredential =
        serviceAccountCredential
            .createScoped(scopes)
            .createDelegated(testUserSpecification.userEmail);
    delegatedUserCredentials.put(testUserSpecification.userEmail, delegatedUserCredential);
    return delegatedUserCredential;
  }

  /**
   * Build a service account credential with "cloud-platform" scope. This requires a service account
   * client secret file. This credential is used, for example, to manipulate the Kubernetes cluster
   * and run deploy scripts.
   *
   * @param serviceAccount
   * @return a service account credential
   */
  public static GoogleCredentials getServiceAccountCredential(
      ServiceAccountSpecification serviceAccount) throws IOException {
    return getServiceAccountCredential(serviceAccount, false);
  }

  /**
   * Build a service account credential with: ("openid", "email", "profile") scopes when
   * withUserLoginScopes is true "cloud-platform" scope when withUserLoginScopes is false This
   * requires a service account client secret file. This credential with user login scopes is used,
   * for example, to call the Buffer Service with its single designated client service account.
   *
   * @param serviceAccount
   * @return a service account credential
   */
  public static GoogleCredentials getServiceAccountCredential(
      ServiceAccountSpecification serviceAccount, boolean withUserLoginScopes) throws IOException {
    if (serviceAccountCredential != null) {
      return serviceAccountCredential;
    }

    List<String> scopes =
        withUserLoginScopes ? userLoginScopes : Collections.singletonList(cloudPlatformScope);

    synchronized (lockServiceAccountCredential) {
      File jsonKey = serviceAccount.jsonKeyFile;
      serviceAccountCredential =
          ServiceAccountCredentials.fromStream(new FileInputStream(jsonKey)).createScoped(scopes);
    }
    return serviceAccountCredential;
  }

  /**
   * Build an application default credential with "cloud-platform" scope.
   *
   * @return an application default credential
   */
  public static GoogleCredentials getApplicationDefaultCredential() throws IOException {
    if (applicationDefaultCredential != null) {
      return applicationDefaultCredential;
    }

    synchronized (lockApplicationDefaultCredential) {
      applicationDefaultCredential =
          GoogleCredentials.getApplicationDefault()
              .createScoped(Collections.singletonList(cloudPlatformScope));
    }
    return applicationDefaultCredential;
  }

  /**
   * Refresh the credential if expired and then return its access token.
   *
   * @param credential
   * @return access token
   */
  public static AccessToken getAccessToken(GoogleCredentials credential) {
    try {
      credential.refreshIfExpired();
      return credential.getAccessToken();
    } catch (IOException ioEx) {
      throw new RuntimeException("Error refreshing access token", ioEx);
    }
  }
}
