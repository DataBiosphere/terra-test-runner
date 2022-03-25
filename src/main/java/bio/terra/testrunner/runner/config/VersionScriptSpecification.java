package bio.terra.testrunner.runner.config;

import bio.terra.testrunner.common.utils.LogsUtils;
import bio.terra.testrunner.runner.VersionScript;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressFBWarnings(
    value = "UUF_UNUSED_PUBLIC_OR_PROTECTED_FIELD",
    justification = "This POJO class is used for easy serialization to JSON using Jackson.")
public class VersionScriptSpecification implements SpecificationInterface {
  public String name = "";

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public Map<String, String> parametersMap = new HashMap<>();

  public String description;

  public Class<? extends VersionScript> scriptClass;

  public static final String scriptsPackage = "scripts.versionscripts";

  VersionScriptSpecification() {}

  /**
   * Validate the version script specification read in from the JSON file. The name is converted
   * into a Java class reference
   */
  public void validate() {
    try {
      Class<?> scriptClassGeneric = Class.forName(scriptsPackage + "." + name);
      scriptClass = (Class<? extends VersionScript>) scriptClassGeneric;
    } catch (ClassNotFoundException | ClassCastException classEx) {
      throw new IllegalArgumentException("Version script class not found: " + name, classEx);
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
