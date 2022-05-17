package bio.terra.testrunner.uploader.config;

import bio.terra.testrunner.common.utils.LogsUtils;
import bio.terra.testrunner.runner.config.SpecificationInterface;
import bio.terra.testrunner.uploader.UploadScript;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@SuppressFBWarnings(
    value = "UWF_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD",
    justification = "This POJO class is used for easy serialization to JSON using Jackson.")
public class UploadScriptSpecification implements SpecificationInterface {
  public String name;
  public String description;

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public Map<String, String> parametersMap;

  private UploadScript scriptClassInstance;

  public static final String scriptsPackage = "scripts.uploadscripts";

  UploadScriptSpecification() {}

  public UploadScript scriptClassInstance() {
    return scriptClassInstance;
  }

  /**
   * Validate the upload script specification read in from the JSON file. The name is converted into
   * a Java class reference.
   */
  public void validate() {
    try {
      Class<?> scriptClassGeneric = Class.forName(scriptsPackage + "." + name);
      Class<? extends UploadScript> scriptClass =
          (Class<? extends UploadScript>) scriptClassGeneric;
      scriptClassInstance = scriptClass.newInstance();
    } catch (ClassNotFoundException | ClassCastException classEx) {
      throw new IllegalArgumentException("Upload script class not found: " + name, classEx);
    } catch (IllegalAccessException | InstantiationException niEx) {
      throw new IllegalArgumentException(
          "Error calling constructor of UploadScript class: " + name, niEx);
    }
  }
  /**
   * Return parametersMap as a JSON string
   *
   * @return a Java String
   */
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  public List<String> getParameters() {
    return Arrays.asList(new String[] {LogsUtils.parametersToString(parametersMap)});
  }
}
