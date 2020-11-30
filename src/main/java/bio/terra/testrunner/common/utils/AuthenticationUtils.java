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
  public static final List<String> userLoginScopes =
      Collections.unmodifiableList(Arrays.asList("openid", "email", "profile"));

  public static final List<String> cloudPlatformScope =
      Collections.unmodifiableList(Arrays.asList("https://www.googleapis.com/auth/cloud-platform"));

  /**
   * Build a domain-wide delegated user credential with the the specified scopes.
   *
   * <p>This requires the email of the user, and a service account that has permissions to get
   * domain-wide delegated user credentials. My understanding is that domain-wide delegation is
   * GSuite-related, not GCP-related, and also not currently Terraform-able.
   *
   * <p>This credential is used, for example, with the Harry Potter users.
   *
   * @param testUser
   * @param scopes
   * @return a domain-wide delegated user credential
   */
  public static GoogleCredentials getDelegatedUserCredential(
      TestUserSpecification testUser, List<String> scopes) throws IOException {
    GoogleCredentials delegatedUserCredential = delegatedUserCredentials.get(testUser.userEmail);
    if (delegatedUserCredential != null) {
      return delegatedUserCredential;
    }

    GoogleCredentials serviceAccountCredential =
        getServiceAccountCredential(testUser.delegatorServiceAccount, cloudPlatformScope);
    delegatedUserCredential =
        serviceAccountCredential.createScoped(scopes).createDelegated(testUser.userEmail);
    delegatedUserCredentials.put(testUser.userEmail, delegatedUserCredential);
    return delegatedUserCredential;
  }

  /**
   * Build a service account credential with the specified scopes. This requires a service account
   * client secret file.
   *
   * <p>Service account credentials are used, for example:
   *
   * <p>- To manipulate the Kubernetes cluster and run deploy scripts (with cloud-platform scope)
   *
   * <p>- To call Buffer Service with its single designated client service account (with user login
   * scopes)
   *
   * @param serviceAccount
   * @param scopes
   * @return a service account credential
   */
  public static GoogleCredentials getServiceAccountCredential(
      ServiceAccountSpecification serviceAccount, List<String> scopes) throws IOException {
    if (serviceAccountCredential != null) {
      return serviceAccountCredential;
    }

    synchronized (lockServiceAccountCredential) {
      File jsonKey = serviceAccount.jsonKeyFile;
      serviceAccountCredential =
          ServiceAccountCredentials.fromStream(new FileInputStream(jsonKey)).createScoped(scopes);
    }
    return serviceAccountCredential;
  }

  /**
   * Build an application default credential with the specified scopes.
   *
   * @param scopes
   * @return an application default credential
   */
  public static GoogleCredentials getApplicationDefaultCredential(List<String> scopes)
      throws IOException {
    if (applicationDefaultCredential != null) {
      return applicationDefaultCredential;
    }

    synchronized (lockApplicationDefaultCredential) {
      applicationDefaultCredential = GoogleCredentials.getApplicationDefault().createScoped(scopes);
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
