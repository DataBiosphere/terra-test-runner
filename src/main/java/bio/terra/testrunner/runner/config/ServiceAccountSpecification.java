package bio.terra.testrunner.runner.config;

import bio.terra.testrunner.common.utils.FileUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.InputStream;
import org.apache.commons.lang3.StringUtils;

public class ServiceAccountSpecification implements SpecificationInterface {
  public String name;
  public String jsonKeyDirectoryPath;
  public String jsonKeyFilename;

  @JsonIgnore public File jsonKeyFile;

  public static final String resourceDirectory = "serviceaccounts";
  public static final String keyDirectoryPathEnvironmentVarName =
      "TEST_RUNNER_SA_KEY_DIRECTORY_PATH";

  ServiceAccountSpecification() {}

  /**
   * Read an instance of this class in from a JSON-formatted file. This method expects that the file
   * name exists in the directory specified by {@link #resourceDirectory}
   *
   * @param resourceFileName file name
   * @return an instance of this class
   */
  public static ServiceAccountSpecification fromJSONFile(String resourceFileName) throws Exception {
    // use Jackson to map the stream contents to a TestConfiguration object
    ObjectMapper objectMapper = new ObjectMapper().enable(JsonParser.Feature.ALLOW_COMMENTS);

    InputStream inputStream =
        FileUtils.getResourceFileHandle(resourceDirectory + "/" + resourceFileName);
    ServiceAccountSpecification serviceAccount =
        objectMapper.readValue(inputStream, ServiceAccountSpecification.class);

    String keyDirectoryPathEnvVarOverride = readKeyDirectoryPathEnvironmentVariable();
    if (keyDirectoryPathEnvVarOverride != null) {
      serviceAccount.jsonKeyDirectoryPath = keyDirectoryPathEnvVarOverride;
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

  /**
   * Validate the service account specification read in from the JSON file. None of the properties
   * should be null.
   */
  public void validate() {
    if (name == null || name.equals("")) {
      throw new IllegalArgumentException("Service account name cannot be empty");
    } else if (StringUtils.isBlank(jsonKeyFilename)) {
      throw new IllegalArgumentException("JSON key file name cannot be empty");
    } else if (StringUtils.isBlank(jsonKeyDirectoryPath)) {
      throw new IllegalArgumentException("JSON key directory path cannot be empty");
    }

    jsonKeyFile = new File(jsonKeyDirectoryPath, jsonKeyFilename);

    if (!jsonKeyFile.exists()) {
      throw new IllegalArgumentException(
          "JSON key file does not exist: (directory)"
              + jsonKeyDirectoryPath
              + ", (filename)"
              + jsonKeyFilename);
    }
  }
}
