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

public final class AuthenticationUtils {

  private AuthenticationUtils() {}

  // the list of scopes we request from end users when they log in. this should always match exactly
  // what the UI requests, so our tests represent actual user behavior
  public static final List<String> userLoginScopes = List.of("openid", "email", "profile");

  public static final List<String> cloudPlatformScope =
      List.of("https://www.googleapis.com/auth/cloud-platform");

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
    GoogleCredentials serviceAccountCredential =
        getServiceAccountCredential(testUser.delegatorServiceAccount, cloudPlatformScope);
    return serviceAccountCredential.createScoped(scopes).createDelegated(testUser.userEmail);
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
    File jsonKey = serviceAccount.jsonKeyFile;
    return ServiceAccountCredentials.fromStream(new FileInputStream(jsonKey)).createScoped(scopes);
  }

  /**
   * Build an application default credential with the specified scopes.
   *
   * @param scopes
   * @return an application default credential
   */
  public static GoogleCredentials getApplicationDefaultCredential(List<String> scopes)
      throws IOException {
    return GoogleCredentials.getApplicationDefault().createScoped(scopes);
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
