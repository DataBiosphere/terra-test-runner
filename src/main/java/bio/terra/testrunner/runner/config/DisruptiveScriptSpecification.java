package bio.terra.testrunner.runner.config;

import bio.terra.testrunner.common.utils.LogsUtils;
import bio.terra.testrunner.runner.DisruptiveScript;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@SuppressFBWarnings(
    value = "UWF_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD",
    justification = "This POJO class is used for easy serialization to JSON using Jackson.")
public class DisruptiveScriptSpecification implements SpecificationInterface {
  public String name;

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public Map<String, String> parametersMap;

  private DisruptiveScript disruptiveScriptClassInstance;

  public static final String disruptiveScriptsPackage = "scripts.disruptivescripts";

  DisruptiveScriptSpecification() {}

  public DisruptiveScript disruptiveScriptClassInstance() {
    return disruptiveScriptClassInstance;
  }

  public void validate() {
    try {
      Class<?> scriptClassGeneric = Class.forName(disruptiveScriptsPackage + "." + name);
      Class<? extends DisruptiveScript> scriptClass =
          (Class<? extends DisruptiveScript>) scriptClassGeneric;
      disruptiveScriptClassInstance = scriptClass.newInstance();
    } catch (ClassNotFoundException | ClassCastException classEx) {
      throw new IllegalArgumentException("Disruptive script class not found: " + name, classEx);
    } catch (IllegalAccessException | InstantiationException niEx) {
      throw new IllegalArgumentException(
          "Error calling constructor of Disruptive Script class: " + name, niEx);
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
