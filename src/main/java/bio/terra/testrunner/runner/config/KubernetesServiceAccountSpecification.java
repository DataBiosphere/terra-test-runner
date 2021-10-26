package bio.terra.testrunner.runner.config;

import bio.terra.testrunner.common.utils.FileUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.InputStream;
import org.apache.commons.lang3.StringUtils;

public class KubernetesServiceAccountSpecification implements SpecificationInterface {
  // Used for In-Cluster Service Account
  public String name;
  public String clientKeyDirectoryPath;
  public String clientKeyFilename;
  public String tokenFilename;

  @JsonIgnore public File clientKeyFile;
  @JsonIgnore public File tokenFile;

  public static final String resourceDirectory = "kubernetesserviceaccounts";
  public static final String keyDirectoryPathEnvironmentVarName =
      "TEST_RUNNER_K8S_SA_KEY_DIRECTORY_PATH";

  public KubernetesServiceAccountSpecification() {}

  /**
   * Read an instance of this class in from a JSON-formatted file. This method expects that the file
   * name exists in the directory specified by {@link #resourceDirectory}
   *
   * @param resourceFileName file name
   * @return an instance of this class
   */
  public static KubernetesServiceAccountSpecification fromJSONFile(String resourceFileName)
      throws Exception {
    // use Jackson to map the stream contents to a TestConfiguration object
    ObjectMapper objectMapper = new ObjectMapper();

    InputStream inputStream =
        FileUtils.getResourceFileHandle(resourceDirectory + "/" + resourceFileName);
    KubernetesServiceAccountSpecification serviceAccount =
        objectMapper.readValue(inputStream, KubernetesServiceAccountSpecification.class);

    String keyDirectoryPathEnvVarOverride = readKeyDirectoryPathEnvironmentVariable();
    if (keyDirectoryPathEnvVarOverride != null) {
      serviceAccount.clientKeyDirectoryPath = keyDirectoryPathEnvVarOverride;
    }

    return serviceAccount;
  }

  protected static String readKeyDirectoryPathEnvironmentVariable() {
    // look for the service account JSON key file in a directory defined by, in order:
    //   1. environment variable
    //   2. service account jsonKeyDirectoryPath property
    String keyDirectoryPathEnvironmentVarValue = System.getenv(keyDirectoryPathEnvironmentVarName);
    return keyDirectoryPathEnvironmentVarValue;
  }

  @Override
  public void validate() {
    if (name == null || name.equals("")) {
      throw new IllegalArgumentException("Service account name cannot be empty");
    } else if (StringUtils.isBlank(clientKeyFilename)) {
      throw new IllegalArgumentException("Client key file name cannot be empty");
    } else if (StringUtils.isBlank(tokenFilename)) {
      throw new IllegalArgumentException("Client token file name cannot be empty");
    } else if (StringUtils.isBlank(clientKeyDirectoryPath)) {
      throw new IllegalArgumentException("Client key directory path cannot be empty");
    }

    clientKeyFile = new File(clientKeyDirectoryPath, clientKeyFilename);
    tokenFile = new File(clientKeyDirectoryPath, tokenFilename);

    if (!clientKeyFile.exists() || !tokenFile.exists()) {
      throw new IllegalArgumentException(
          "Kubernetes SA Client key or token file does not exist: (directory)"
              + clientKeyDirectoryPath
              + ", (filename)"
              + clientKeyFilename
              + ", (filename)"
              + tokenFilename);
    }
  }
}
